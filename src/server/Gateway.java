package server;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import crypto.Encrypter;
import crypto.Hasher;
import crypto.MyKeyGenerator;
import datagrams.Message;
import datagrams.Permission;
import functional_interfaces.PermitFirewall;
import model.Car;
import model.EconomicCar;
import model.ExecutiveCar;
import model.IntermediaryCar;
import model.Keys;
import model.RSAKeys;
import model.User;
import server.authentication.AuthInterface;
import server.storage.ServerRole;
import server.storage.StorageInterface;

public class Gateway implements GatewayInterface {

	private static Keys myKeys;
	private static Map<Integer, Keys> myClientKeys;
	private static Map<StorageInterface, RSAKeys> storageKeys;
	private static RSAKeys currentStorKeys;
	
	static String authenticationHostName = "Authentication";
	static String storageHostName[] = {"Storage0","Storage1","Storage2"};
	static AuthInterface authServer;
	static StorageInterface storServer;
	static List<StorageInterface> stores;
	public static Registry register;
	
	private static List<Permission> permissions;
	
	BalancingStrategy strategy = BalancingStrategy.HASHING;
	static StorageInterface leastStorageServer;
	static int indexRR = 0;
	// ips fake pra exemplificação do consistent hashing
							//  40.180.45.45 | 26.95.199.60 |40.180.45.45 |192.168.10.222 | 192.168.10.222 | 26.95.199.60
	static String[] ipClient = {"76.76.40.25", "192.0.2.44", "203.0.113.2", "25.102.140.11", "198.51.100.24", "26.95.199.60" };
	static Map<BigInteger, String> serverMap = new HashMap<>();
    static List<BigInteger> sortedHashes = new ArrayList<>();
	
	public static void main(String[] args) throws RemoteException {
		
		Gateway gateway = new Gateway();
		stores = new ArrayList<>();
		myKeys = new Keys();
		myClientKeys = new HashMap<Integer, Keys>();
		storageKeys = new HashMap<StorageInterface, RSAKeys>();
		permissions = new ArrayList<Permission>();
		Gateway.setPermissions();
		
		// geração de chaves
		myKeys.setVernamKey(MyKeyGenerator.generateKeyVernam());
		myKeys.setAesKey(MyKeyGenerator.generateKeyAes());
		myKeys.setHMACKey(MyKeyGenerator.generateKeyHMAC());
		RSAKeys myRsaKeys = MyKeyGenerator.generateKeysRSA();
		// nao adiciona a chave privada ainda pq ainda vai enviar pros servers
		myKeys.setRsaKeys(new RSAKeys(myRsaKeys.getPublicKey(), myRsaKeys.getnMod()));
		
		try {
			Registry authRegister = LocateRegistry.getRegistry("localhost", 5001);
			authServer = (AuthInterface) authRegister.lookup(authenticationHostName);
			
			Registry stgRegister = LocateRegistry.getRegistry("localhost", 5002);
			storServer = (StorageInterface) stgRegister.lookup(storageHostName[0]);
			storServer.addNewClientKeys(myKeys);
			storageKeys.put(storServer, storServer.getRSAKeys());
			stores.add(storServer);
			
			stgRegister = LocateRegistry.getRegistry("localhost", 5003);
			storServer = (StorageInterface) stgRegister.lookup(storageHostName[1]);
			storServer.addNewClientKeys(myKeys);
			storageKeys.put(storServer, storServer.getRSAKeys());
			stores.add(storServer);

			stgRegister = LocateRegistry.getRegistry("localhost", 5004);
			storServer = (StorageInterface) stgRegister.lookup(storageHostName[2]);
			storServer.addNewClientKeys(myKeys);
			storageKeys.put(storServer, storServer.getRSAKeys());
			stores.add(storServer);

			storServer = stores.get(0);
			myKeys.getRsaKeys().setPrivateKey(myRsaKeys.getPrivateKey()); // agora sim add a chave privada dps de enviar pro server sem
			currentStorKeys = storageKeys.get(storServer);
			
			GatewayInterface protocol = (GatewayInterface) UnicastRemoteObject.exportObject(gateway, 0);
			
			//System.setProperty("java.rmi.server.hostname", "10.215.34.54");
			//System.setProperty("java.security.police", "java.policy");
			
	
			register = LocateRegistry.createRegistry(5000);
			register.rebind("Gateway", protocol);
			
			System.out.println("Gateway ligado...");
			
		} catch (RemoteException | NotBoundException e) {
			e.printStackTrace();
		}
		
		for(StorageInterface store : stores) {
			addStorageServer(store.getIpServer());
		}
		
	}

	@Override
	public void register(User newUser) {
		if(Gateway.getPermission(5001)) {
			try {
				authServer.registerUser(newUser);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public User login(String cpf, String password) {
		if(Gateway.getPermission(5001)) {
			try {
				User connected = authServer.loginUser(cpf, password);
				return connected;
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public void addCar(Car newCar) throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.LEADER) {
				String hmac = Hasher.hMac(myKeys.getHMACKey(), newCar.toString());
				String msgEncrypted = Encrypter.fullEncrypt(myKeys, newCar.toString());
				String signature = Encrypter.signMessage(myKeys, hmac);
				
				storServer.receiveMessage(new Message<String>(5, msgEncrypted, signature));
				
				System.out.println("Carro adicionado com sucesso.");
			}
			else if(store.getRole() == ServerRole.OUTOFORDER) {
				storServer = store.startElections();
				currentStorKeys = storageKeys.get(storServer);
				this.addCar(newCar);
			}
		}
	}
	
	public void editCar(String renavam, Car editedCar) throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.LEADER) {
				String hmac = Hasher.hMac(myKeys.getHMACKey(), editedCar.toString());
				String msgEncrypted = Encrypter.fullEncrypt(myKeys, editedCar.toString());
				String signature = Encrypter.signMessage(myKeys, hmac);
				
				storServer.receiveMessage(new Message<String>(6, msgEncrypted, signature));
				
				System.out.println("Carro de renavam " + renavam + " editado com sucesso.");
			}
			else if(store.getRole() == ServerRole.OUTOFORDER) {
				storServer = store.startElections();
				currentStorKeys = storageKeys.get(storServer);
				this.editCar(renavam, editedCar);
			}
		}
	}

	public void deleteCar(String renavam) throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.LEADER) {
				String hmac = Hasher.hMac(myKeys.getHMACKey(), renavam);
				String msgEncrypted = Encrypter.fullEncrypt(myKeys, renavam);
				String signature = Encrypter.signMessage(myKeys, hmac);
				
				storServer.receiveMessage(new Message<String>(7, msgEncrypted, signature));
				System.out.println("Carro de renavam " + renavam + " deletado com sucesso.");
			}
			else if(store.getRole() == ServerRole.OUTOFORDER) {
				storServer = store.startElections();
				currentStorKeys = storageKeys.get(storServer);
				this.deleteCar(renavam);
			}
		}
	}
	
	public void deleteCars(String name) throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.LEADER) {
				String hmac = Hasher.hMac(myKeys.getHMACKey(), name);
				String msgEncrypted = Encrypter.fullEncrypt(myKeys, name);
				String signature = Encrypter.signMessage(myKeys, hmac);
				
				storServer.receiveMessage(new Message<String>(777, msgEncrypted, signature));
				
				System.out.println("Todos os carros " + name + " deletados com sucesso.");
			}
			else if(store.getRole() == ServerRole.OUTOFORDER) {
				storServer = store.startElections();
				currentStorKeys = storageKeys.get(storServer);
				this.deleteCars(name);
			}
		}
	}
	
	public List<Car> listCars() throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		boolean leaderOn = true;
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.OUTOFORDER) {
				leaderOn = false;
				storServer = store;
			}
		}
		if(leaderOn) {
			System.out.println("Lista de carros enviada.");
			
			String hmac = Hasher.hMac(myKeys.getHMACKey(), "push to pull");
			String msgEncrypted = Encrypter.fullEncrypt(myKeys, "push to pull");
			String signature = Encrypter.signMessage(myKeys, hmac);
			
			Message<String> response;
			
			if(strategy == BalancingStrategy.WLC) {
				leastStorageServer = storServer;
				currentStorKeys = storageKeys.get(leastStorageServer);
				
				// balanceamento de carga vai mudar de quem o gateway recebe mensagem
				for(StorageInterface store : stores) {
					if(store.getConnectionNumber() < leastStorageServer.getConnectionNumber() && store.getConnectionWeight() != leastStorageServer.getConnectionWeight()) {
						leastStorageServer = store;
						currentStorKeys = storageKeys.get(leastStorageServer);
						break;
					}
				}
				
				response = leastStorageServer.receiveMessage(new Message<String>(111, msgEncrypted, signature));
				leastStorageServer.incrementConnectionNumber();
			} else if (strategy == BalancingStrategy.HASHING) {
				SecureRandom rd = new SecureRandom();
				int ind = rd.nextInt(6);
				
				System.out.println("--------------------------------");
				System.out.println("IP que está solicitando: " + ipClient[ind]);
				String unhashedServer = getServer(ipClient[ind]);
				
				for(StorageInterface store : stores) {
					if(Hasher.hashIp(store.getIpServer()).equals(Hasher.hashIp(unhashedServer))) {
						System.out.println("Endereço IP do Server destinatário: " + store.getIpServer());
						System.out.println("--------------------------------");
						storServer = store;
						
					}
				}
				currentStorKeys = storageKeys.get(storServer);
				
				response = storServer.receiveMessage(new Message<String>(111, msgEncrypted, signature));
			} else { // Rouding Robin
				storServer = stores.get(indexRR);
				indexRR = (indexRR + 1) % 3;
				currentStorKeys = storageKeys.get(storServer);
				
				response = storServer.receiveMessage(new Message<String>(111, msgEncrypted, signature));
				storServer.incrementRR();
			}
			
			//Message<String> response = storServer.receiveMessage(new Message<String>(111, msgEncrypted, signature));
			
			String decryptedMsg = Encrypter.fullDecrypt(myKeys, response.getContent());
			String realHMAC = Hasher.hMac(myKeys.getHMACKey(), decryptedMsg);
				
			boolean validSignature = Encrypter.verifySignature(currentStorKeys, realHMAC, response.getMessageSignature());
			
			List<Car> findedCars = new ArrayList<Car>();
			if(validSignature) {
				String stringCars[] = decryptedMsg.split("¬");
				for(String stringCar : stringCars) {
					if(stringCar != "") {
						String partsCar[] = stringCar.split("°");
						findedCars.add(new Car(partsCar[0], partsCar[1], Integer.parseInt(partsCar[2]), partsCar[3], Double.parseDouble(partsCar[4])));
					}
				}
				
				return findedCars;
			} else {
				System.out.println("Assinatura incorreta. Servidor inválido.");
			}
			
		}
		storServer = storServer.startElections();
		currentStorKeys = storageKeys.get(storServer);
		return this.listCars();
	}

	public List<Car> listCars(int category) throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		boolean leaderOn = true;
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.OUTOFORDER) {
				leaderOn = false;
				storServer = store;
			}
		}
		if(leaderOn) {
			System.out.println("Lista de carros da categoria " + category + " enviada.");

			String hmac = Hasher.hMac(myKeys.getHMACKey(), String.valueOf(category));
			String msgEncrypted = Encrypter.fullEncrypt(myKeys, String.valueOf(category));
			String signature = Encrypter.signMessage(myKeys, hmac);

			Message<String> response;
			if(strategy == BalancingStrategy.WLC) {
				leastStorageServer = storServer;
				currentStorKeys = storageKeys.get(leastStorageServer);
				
				// balanceamento de carga vai mudar de quem o gateway recebe mensagem
				for(StorageInterface store : stores) {
					if(store.getConnectionNumber() < leastStorageServer.getConnectionNumber() && store.getConnectionWeight() != leastStorageServer.getConnectionWeight()) {
						leastStorageServer = store;
						currentStorKeys = storageKeys.get(leastStorageServer);
						break;
					}
				}
				
				response = leastStorageServer.receiveMessage(new Message<String>(1, msgEncrypted, signature));
				leastStorageServer.incrementConnectionNumber();
			} else if (strategy == BalancingStrategy.HASHING) {
				SecureRandom rd = new SecureRandom();
				int ind = rd.nextInt(6);
				
				System.out.println("--------------------------------");
				System.out.println("IP que está solicitando: " + ipClient[ind]);
				String unhashedServer = getServer(ipClient[ind]);
				
				for(StorageInterface store : stores) {
					if(Hasher.hashIp(store.getIpServer()).equals(Hasher.hashIp(unhashedServer))) {
						System.out.println("oewww");
						System.out.println("Vai pro server: " + store.getIpServer());
						System.out.println("--------------------------------");
						storServer = store;
						
					}
				}
				currentStorKeys = storageKeys.get(storServer);
				
				response = storServer.receiveMessage(new Message<String>(1, msgEncrypted, signature));
			} else { // Rouding Robin
				storServer = stores.get(indexRR);
				indexRR = (indexRR + 1) % 3;
				currentStorKeys = storageKeys.get(storServer);
				
				response = storServer.receiveMessage(new Message<String>(1, msgEncrypted, signature));
				storServer.incrementRR();
			}
			
			//Message<String> response = storServer.receiveMessage(new Message<String>(1, msgEncrypted, signature));
			
			String decryptedMsg = Encrypter.fullDecrypt(myKeys, response.getContent());
			String realHMAC = Hasher.hMac(myKeys.getHMACKey(), decryptedMsg);
				
			boolean validSignature = Encrypter.verifySignature(currentStorKeys, realHMAC, response.getMessageSignature());
			
			List<Car> findedCars = new ArrayList<Car>();
			if(validSignature) {
				String stringCars[] = decryptedMsg.split("¬");
				for(String stringCar : stringCars) {
					if(stringCar != "") {
						String partsCar[] = stringCar.split("°");
						findedCars.add(new Car(partsCar[0], partsCar[1], Integer.parseInt(partsCar[2]), partsCar[3], Double.parseDouble(partsCar[4])));
					}
				}
				
				return findedCars;
			} else {
				System.out.println("Assinatura incorreta. Servidor inválido.");
			}
		} 
		storServer = storServer.startElections();
		currentStorKeys = storageKeys.get(storServer);
		return this.listCars(category);
	}
	
	public Car searchCar(String renavam, boolean buy) throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		boolean leaderOn = true;
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.OUTOFORDER) {
				leaderOn = false;
				storServer = store;
			}
		}
		if(leaderOn) {
			System.out.println("Carro encontrado com sucesso!");
			
			String hmac = Hasher.hMac(myKeys.getHMACKey(), renavam);
			String msgEncrypted = Encrypter.fullEncrypt(myKeys, renavam);
			String signature = Encrypter.signMessage(myKeys, hmac);
			
			Message<String> response;
			if(!buy) { // se nao for pra compra, ele alterna
				if(strategy == BalancingStrategy.WLC) {
					leastStorageServer = storServer;
					currentStorKeys = storageKeys.get(leastStorageServer);
					
					// balanceamento de carga vai mudar de quem o gateway recebe mensagem
					for(StorageInterface store : stores) {
						if(store.getConnectionNumber() < leastStorageServer.getConnectionNumber() && store.getConnectionWeight() != leastStorageServer.getConnectionWeight()) {
							leastStorageServer = store;
							currentStorKeys = storageKeys.get(leastStorageServer);
							break;
						}
					}
					
					response = leastStorageServer.receiveMessage(new Message<String>(2, msgEncrypted, signature));
					leastStorageServer.incrementConnectionNumber();
				} else if (strategy == BalancingStrategy.HASHING) {
					SecureRandom rd = new SecureRandom();
					int ind = rd.nextInt(6);
					
					System.out.println("--------------------------------");
					System.out.println("IP que está solicitando: " + ipClient[ind]);
					String unhashedServer = getServer(ipClient[ind]);
					
					for(StorageInterface store : stores) {
						if(Hasher.hashIp(store.getIpServer()).equals(Hasher.hashIp(unhashedServer))) {
							System.out.println("oewww");
							System.out.println("Vai pro server: " + store.getIpServer());
							System.out.println("--------------------------------");
							storServer = store;
							
						}
					}
					currentStorKeys = storageKeys.get(storServer);
					
					response = storServer.receiveMessage(new Message<String>(2, msgEncrypted, signature));
				} else { // Rouding Robin
					storServer = stores.get(indexRR);
					indexRR = (indexRR + 1) % 3;
					currentStorKeys = storageKeys.get(storServer);
					
					response = storServer.receiveMessage(new Message<String>(2, msgEncrypted, signature));
					storServer.incrementRR();
				}
			} else {
				response = storServer.receiveMessage(new Message<String>(2, msgEncrypted, signature));
			}
			
			
			//Message<String> response = storServer.receiveMessage(new Message<String>(2, msgEncrypted, signature));
			
			String decryptedMsg = Encrypter.fullDecrypt(myKeys, response.getContent());
			String realHMAC = Hasher.hMac(myKeys.getHMACKey(), decryptedMsg);
				
			boolean validSignature = Encrypter.verifySignature(currentStorKeys, realHMAC, response.getMessageSignature());
			
			if(validSignature) {
				String partsCar[] = decryptedMsg.split("°");
				Car findedCar = new Car(partsCar[0], partsCar[1], Integer.parseInt(partsCar[2]), partsCar[3], Double.parseDouble(partsCar[4]));
				
				if(!findedCar.getName().equals("null")) {
					return findedCar;
				} else {
					System.out.println("Carro não encontrado.");
				}
				
			} else {
				System.out.println("Assinatura incorreta. Servidor inválido.");
			}
		}
		storServer = storServer.startElections();
		currentStorKeys = storageKeys.get(storServer);
		return this.searchCar(renavam, false);
	}

	public List<Car> searchCars(String name) throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		boolean leaderOn = true;
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.OUTOFORDER) {
				leaderOn = false;
				storServer = store;
			}
		}
		if(leaderOn) {
			System.out.println("Lista de carros encontrada com sucesso!");

			String hmac = Hasher.hMac(myKeys.getHMACKey(), name);
			String msgEncrypted = Encrypter.fullEncrypt(myKeys, name);
			String signature = Encrypter.signMessage(myKeys, hmac);
			
			Message<String> response;
			if(strategy == BalancingStrategy.WLC) {
				leastStorageServer = storServer;
				currentStorKeys = storageKeys.get(leastStorageServer);
				
				// balanceamento de carga vai mudar de quem o gateway recebe mensagem
				for(StorageInterface store : stores) {
					if(store.getConnectionNumber() < leastStorageServer.getConnectionNumber() && store.getConnectionWeight() != leastStorageServer.getConnectionWeight()) {
						leastStorageServer = store;
						currentStorKeys = storageKeys.get(leastStorageServer);
						break;
					}
				}
				
				response = leastStorageServer.receiveMessage(new Message<String>(222, msgEncrypted, signature));
				leastStorageServer.incrementConnectionNumber();
			} else if (strategy == BalancingStrategy.HASHING) {
				SecureRandom rd = new SecureRandom();
				int ind = rd.nextInt(6);
				
				System.out.println("--------------------------------");
				System.out.println("IP que está solicitando: " + ipClient[ind]);
				String unhashedServer = getServer(ipClient[ind]);
				
				for(StorageInterface store : stores) {
					if(Hasher.hashIp(store.getIpServer()).equals(Hasher.hashIp(unhashedServer))) {
						System.out.println("oewww");
						System.out.println("Vai pro server: " + store.getIpServer());
						System.out.println("--------------------------------");
						storServer = store;
						
					}
				}
				currentStorKeys = storageKeys.get(storServer);
				
				response = storServer.receiveMessage(new Message<String>(222, msgEncrypted, signature));
			} else { // Rouding Robin
				storServer = stores.get(indexRR);
				indexRR = (indexRR + 1) % 3;
				currentStorKeys = storageKeys.get(storServer);
				
				response = storServer.receiveMessage(new Message<String>(222, msgEncrypted, signature));
				storServer.incrementRR();
			}
			
			//Message<String> response = storServer.receiveMessage(new Message<String>(222, msgEncrypted, signature));

			String decryptedMsg = Encrypter.fullDecrypt(myKeys, response.getContent());
			String realHMAC = Hasher.hMac(myKeys.getHMACKey(), decryptedMsg);
				
			boolean validSignature = Encrypter.verifySignature(currentStorKeys, realHMAC, response.getMessageSignature());
			
			List<Car> findedCars = new ArrayList<Car>();
			if(validSignature) {
				String stringCars[] = decryptedMsg.split("¬");
				for(String stringCar : stringCars) {
					if(stringCar != "") {
						String partsCar[] = stringCar.split("°");
						findedCars.add(new Car(partsCar[0], partsCar[1], Integer.parseInt(partsCar[2]), partsCar[3], Double.parseDouble(partsCar[4])));
					}
				}
				
				return findedCars;
			} else {
				System.out.println("Assinatura incorreta. Servidor inválido.");
			}
		}
		storServer = storServer.startElections();
		currentStorKeys = storageKeys.get(storServer);
		return this.searchCars(name);
	}
	
	public Car buyCar(String renavam) throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.LEADER) {
				System.out.println("Carro de renavam " + renavam + " foi comprado.");

				String hmac = Hasher.hMac(myKeys.getHMACKey(), renavam);
				String msgEncrypted = Encrypter.fullEncrypt(myKeys, renavam);
				String signature = Encrypter.signMessage(myKeys, hmac);
				
				Message<String> response = storServer.receiveMessage(new Message<String>(3, msgEncrypted, signature));

				String decryptedMsg = Encrypter.fullDecrypt(myKeys, response.getContent());
				String realHMAC = Hasher.hMac(myKeys.getHMACKey(), decryptedMsg);
					
				boolean validSignature = Encrypter.verifySignature(currentStorKeys, realHMAC, response.getMessageSignature());
				
				if(validSignature) {
					String partsCar[] = decryptedMsg.split("°");
					Car findedCar = new Car(partsCar[0], partsCar[1], Integer.parseInt(partsCar[2]), partsCar[3], Double.parseDouble(partsCar[4]));
					
					if(!findedCar.getName().equals("null")) {
						return findedCar;
					} else {
						System.out.println("Carro não encontrado.");
					}
					
				} else {
					System.out.println("Assinatura incorreta. Servidor inválido.");
				}
				
			}
			else if(store.getRole() == ServerRole.OUTOFORDER) {
				storServer = storServer.startElections();
				currentStorKeys = storageKeys.get(storServer);
			}
		}
		return null;
	}
	
	public int getAmount(int category) throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		String hmac = Hasher.hMac(myKeys.getHMACKey(), String.valueOf(category));
		String msgEncrypted = Encrypter.fullEncrypt(myKeys, String.valueOf(category));
		String signature = Encrypter.signMessage(myKeys, hmac);
		
		Message<String> response;
		if(strategy == BalancingStrategy.WLC) {
			leastStorageServer = storServer;
			currentStorKeys = storageKeys.get(leastStorageServer);
			
			// balanceamento de carga vai mudar de quem o gateway recebe mensagem
			for(StorageInterface store : stores) {
				if(store.getConnectionNumber() < leastStorageServer.getConnectionNumber() && store.getConnectionWeight() != leastStorageServer.getConnectionWeight()) {
					leastStorageServer = store;
					currentStorKeys = storageKeys.get(leastStorageServer);
					break;
				}
			}
			
			response = leastStorageServer.receiveMessage(new Message<String>(4, msgEncrypted, signature));
			leastStorageServer.incrementConnectionNumber();
		} else if (strategy == BalancingStrategy.HASHING) {
			SecureRandom rd = new SecureRandom();
			int ind = rd.nextInt(6);
			
			System.out.println("--------------------------------");
			System.out.println("IP que está solicitando: " + ipClient[ind]);
			String unhashedServer = getServer(ipClient[ind]);
			
			for(StorageInterface store : stores) {
				if(Hasher.hashIp(store.getIpServer()).equals(Hasher.hashIp(unhashedServer))) {
					System.out.println("oewww");
					System.out.println("Vai pro server: " + store.getIpServer());
					System.out.println("--------------------------------");
					storServer = store;
					
				}
			}
			currentStorKeys = storageKeys.get(storServer);
			
			
			response = storServer.receiveMessage(new Message<String>(4, msgEncrypted, signature));
		} else { // Rouding Robin
			storServer = stores.get(indexRR);
			indexRR = (indexRR + 1) % 3;
			currentStorKeys = storageKeys.get(storServer);
			
			response = storServer.receiveMessage(new Message<String>(4, msgEncrypted, signature));
			storServer.incrementRR();
		}
		
		//Message<String> response = storServer.receiveMessage(new Message<String>(4, msgEncrypted, signature));
		//int amount = responseInt.getContent();
		
		String decryptedMsg = Encrypter.fullDecrypt(myKeys, response.getContent());
		String realHMAC = Hasher.hMac(myKeys.getHMACKey(), decryptedMsg);
			
		boolean validSignature = Encrypter.verifySignature(currentStorKeys, realHMAC, response.getMessageSignature());
		
		if(validSignature) {
			return Integer.parseInt(decryptedMsg);
		}

		return 0;

	}

	@Override
	public void putToSleep() throws RemoteException {
		for(StorageInterface store : stores) {
			if(store.getRole() == ServerRole.LEADER) {
				store.setRole(ServerRole.OUTOFORDER);
			}
		}
	}
	
	@Override
	public void addNewClientKeys(Integer clientNumber, Keys newClientKeys) throws RemoteException {
		myClientKeys.put(clientNumber, newClientKeys);
		System.out.println("Cliente: " + clientNumber + ", Chave pública: " + newClientKeys.getRsaKeys().getPublicKey());
		System.out.println("Chave privada: " + newClientKeys.getRsaKeys().getPrivateKey());
	}
	
	@Override
	public RSAKeys getRSAKeys() throws RemoteException {
		return new RSAKeys(myKeys.getRsaKeys().getPublicKey(), myKeys.getRsaKeys().getnMod());
	}
	
	@Override
	public Message<String> receiveMessage(Message<String> msg) throws RemoteException{
		// permissões p serviço da loja
		if(Gateway.getPermission(5002) || Gateway.getPermission(5003) || Gateway.getPermission(5004)) {
			Keys currentClient = myClientKeys.get(msg.getClientSendingMsg());
			System.out.println("chave publica do rsa:" + currentClient.getRsaKeys().getPublicKey());
			
			if(currentClient != null) {
				String decryptedMsg = Encrypter.fullDecrypt(currentClient, msg.getContent());
				String realHMAC;
				try {
					realHMAC = Hasher.hMac(currentClient.getHMACKey(), decryptedMsg);
					
					boolean validSignature = Encrypter.verifySignature(currentClient.getRsaKeys(), realHMAC, msg.getMessageSignature());
					
					String hmac;
					String msgEncrypted;
					String signature;
					String toEncrypt;
					if(validSignature) {
						switch(msg.getOperation()) {
						case 1:
							List<Car> response = this.listCars(Integer.parseInt(decryptedMsg)); // MUDARR O NUMERO PRO NUMERO DA MSG
							
							toEncrypt = "";
							for(Car car : response) {
								toEncrypt = toEncrypt + "¬" + car.toString();
							}
							
							hmac = Hasher.hMac(currentClient.getHMACKey(), toEncrypt);
							msgEncrypted = Encrypter.fullEncrypt(currentClient, toEncrypt);
							signature = Encrypter.signMessage(myKeys, hmac);
							
							return new Message<String>(1, msgEncrypted, signature);
						case 111: 
							List<Car> response2 = this.listCars();
							
							toEncrypt = "";
							for(Car car : response2) {
								toEncrypt = toEncrypt + "¬" + car.toString();
							}
							
							hmac = Hasher.hMac(currentClient.getHMACKey(), toEncrypt);
							msgEncrypted = Encrypter.fullEncrypt(currentClient, toEncrypt);
							signature = Encrypter.signMessage(myKeys, hmac);
							
							return new Message<String>(111, msgEncrypted, signature);
						case 2:
							Car response3 = this.searchCar(decryptedMsg, false);
							
							hmac = Hasher.hMac(currentClient.getHMACKey(), response3.toString());
							msgEncrypted = Encrypter.fullEncrypt(currentClient, response3.toString());
							signature = Encrypter.signMessage(myKeys, hmac);
							
							return new Message<String>(2, msgEncrypted, signature);
						case 2000: // case específico para pesquisar nem alterar o balanceamento
							Car response2000 = this.searchCar(decryptedMsg, true);
							
							hmac = Hasher.hMac(currentClient.getHMACKey(), response2000.toString());
							msgEncrypted = Encrypter.fullEncrypt(currentClient, response2000.toString());
							signature = Encrypter.signMessage(myKeys, hmac);
							
							return new Message<String>(2, msgEncrypted, signature);
						case 222:
							List<Car> response4 = this.searchCars(decryptedMsg);
							
							toEncrypt = ""; 
							for(Car car : response4) {
								toEncrypt = toEncrypt + "¬" + car.toString();
							}
							
							hmac = Hasher.hMac(currentClient.getHMACKey(), toEncrypt);
							msgEncrypted = Encrypter.fullEncrypt(currentClient, toEncrypt);
							signature = Encrypter.signMessage(myKeys, hmac);
							
							return new Message<String>(222, msgEncrypted, signature);
						case 3:
							Car response5 = this.buyCar(decryptedMsg);
							
							hmac = Hasher.hMac(currentClient.getHMACKey(), response5.toString());
							msgEncrypted = Encrypter.fullEncrypt(currentClient, response5.toString());
							signature = Encrypter.signMessage(myKeys, hmac);
							
							return new Message<String>(3, msgEncrypted, signature);
						case 4:
							Integer response6 = this.getAmount(Integer.parseInt(decryptedMsg));
							
							hmac = Hasher.hMac(currentClient.getHMACKey(), String.valueOf(response6));
							msgEncrypted = Encrypter.fullEncrypt(currentClient, String.valueOf(response6));
							signature = Encrypter.signMessage(myKeys, hmac);
							
							return new Message<String>(4, msgEncrypted, signature);
						case 5:
							String[] carPart = decryptedMsg.split("°");
							int typeOfCar = Integer.parseInt(carPart[2]);
							switch(typeOfCar) {
							case 1:
								this.addCar(new EconomicCar(carPart[0], carPart[1], typeOfCar, carPart[3], Double.parseDouble(carPart[4])));
								break;
							case 2:
								this.addCar(new IntermediaryCar(carPart[0], carPart[1], typeOfCar, carPart[3], Double.parseDouble(carPart[4])));
								break;
							case 3:
								this.addCar(new ExecutiveCar(carPart[0], carPart[1], typeOfCar, carPart[3], Double.parseDouble(carPart[4])));
								break;
							default:
								System.out.println("Tipo inválido.");
							}
							return null; // fica tipo o return void
						case 6:
							String[] carPart2 = decryptedMsg.split("°");
							String toEditRenavam = carPart2[0];
							this.editCar(toEditRenavam, new Car(toEditRenavam, carPart2[1], Integer.parseInt(carPart2[2]), carPart2[3], Double.parseDouble(carPart2[4])));
							return null; // fica tipo return void
						case 7:
							this.deleteCar(decryptedMsg);
							return null; // return void
						case 777:
							this.deleteCars(decryptedMsg);
							return null; // return void
						default: 
							System.out.println("Opção inválida.");
						}
					} else {
						System.out.println("Assinatura inválida. Cliente não autorizado.");
					}
					
				} catch (InvalidKeyException | NoSuchAlgorithmException | UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}
			}
		}
		
		return null;
	}
	
	public static boolean getPermission(int destPort) {	
		String clientHost = "";
		
		
		try {
			clientHost = RemoteServer.getClientHost();
		} catch (ServerNotActiveException e1) {
			e1.printStackTrace();
		}
		//clientHost = getIp();

		//System.out.println(clientHost);
		
		for(Permission perm : permissions) {
			if(perm.getSourceIp().equals(clientHost) && perm.getDestinationPort() == destPort) {
				String aux;
				if(perm.isAllow()) {
					aux = "permitido";
				} else {
					aux = "negado";
				}
				
				System.out.println("------------------------");
				System.out.println("Firewall --> Pacote " + aux + ". Acesso: " + perm.getName() + ", source: " + clientHost);
				return true;
			}
		}
		
		
		System.out.println("------------------------");
		System.out.println("Firewall --> Pacote negado. Source: " + clientHost);
		
		return false; // negou acesso
	}
	
	public static void setPermissions() {
		PermitFirewall permitAccess = (str) -> {
			permissions.add(new Permission(str, "127.0.0.1", 5001, "Autenticação", true));
			permissions.add(new Permission(str, "127.0.0.2", 5002, "Loja1", true));
			permissions.add(new Permission(str, "127.0.0.3", 5003, "Loja2", true));
			permissions.add(new Permission(str, "127.0.0.4", 5004, "Loja3", true));
		};
		
		//vinicius client
		permitAccess.permit("192.168.144.112");
		// ryan client
		permitAccess.permit("192.168.144.218");
		// ryan client 22/08/2024
		permitAccess.permit("192.168.1.4");
		// vinicius client 22/08/2024
		permitAccess.permit("26.15.5.193");

	}
	
	private static String getIp() {
        String ip = "127.0.0.1";

        try {
            var interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                if(iface.isLoopback() || !iface.isUp()) {
                    continue;
                }

                var addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()){
   
                    InetAddress addr = addresses.nextElement();

                    if (addr instanceof Inet4Address) {
						ip = addr.getHostAddress();
						if(ip.startsWith("10.")) { // vai pegar o da ufersa
							return ip;	
						}
					}
                }

            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        return ip;
    }
	
	private static void addStorageServer(String server) {
        BigInteger hash = Hasher.hashIp(server);
        serverMap.put(hash, server);
        sortedHashes.add(hash);
        Collections.sort(sortedHashes);
    }

    /*private static void removeStorageServer(String server) {
        BigInteger hash = Hasher.hashIp(server);
        serverMap.remove(hash);
        sortedHashes.remove(hash);
    }*/
    
    private static String getServer(String ip) {
        BigInteger hash = Hasher.hashIp(ip);
        int index = Collections.binarySearch(sortedHashes, hash);

        if (index < 0) {
            index = -index - 1;
        }

        if (index >= sortedHashes.size()) {
            index = 0;
        }

        return serverMap.get(sortedHashes.get(index));
    }

	
}
