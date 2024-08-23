package client;

import java.io.UnsupportedEncodingException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import crypto.Encrypter;
import crypto.Hasher;
import crypto.MyKeyGenerator;
import datagrams.Message;
import model.Car;
import model.Client;
import model.EconomicCar;
import model.Employee;
import model.ExecutiveCar;
import model.IntermediaryCar;
import model.Keys;
import model.RSAKeys;
import model.User;
import server.GatewayInterface;
import server.storage.DatabaseInterface;
import server.storage.StorageInterface;
import java.util.concurrent.*;

public class ProcessClient {
	
	static int clientNumber;
	static Keys myKeys;
	static RSAKeys gatewayRSAKeys;
	
	private static boolean connected = false;
	private static int timer = 60;
	private static Scanner scan;
	private static GatewayInterface gateway;
	private static User connectedUser;
	private static List<Car> myCars;

	private static ScheduledExecutorService scheduler;
	
	// pra demonstração do firewall
	private static StorageInterface storServer;
	private static DatabaseInterface dataServer;
	
	private static int passwordTries = 3;
	
	public static void main(String[] args) {
		
		clientNumber = 0;
		myKeys = new Keys();
		scan = new Scanner(System.in);
		myCars = new ArrayList<Car>();
		
		// cada cliente vai ter suas chaves
		myKeys.setVernamKey(MyKeyGenerator.generateKeyVernam());
		myKeys.setAesKey(MyKeyGenerator.generateKeyAes());
		myKeys.setHMACKey(MyKeyGenerator.generateKeyHMAC());
		RSAKeys myRsaKeys = MyKeyGenerator.generateKeysRSA();
		// nao adiciona a chave privada ainda pq ainda vai enviar pro gateway
		myKeys.setRsaKeys(new RSAKeys(myRsaKeys.getPublicKey(), myRsaKeys.getnMod()));
		
		try {
			Registry gatRegister = LocateRegistry.getRegistry("192.168.1.4", 5000);
			gateway = (GatewayInterface) gatRegister.lookup("Gateway");
			
			// pra demonstração do firewall
			Registry storRegister = LocateRegistry.getRegistry("192.168.1.4", 5002);
			storServer = (StorageInterface) storRegister.lookup("Storage0");
			
			// pra demonstração do firewall
			Registry dataRegister = LocateRegistry.getRegistry("192.168.1.4", 5010);
			dataServer = (DatabaseInterface) dataRegister.lookup("Database1");
			
			gateway.addNewClientKeys(clientNumber, myKeys); // manda suas chaves pro server
			gatewayRSAKeys = gateway.getRSAKeys();	// pega as do gateway
			myKeys.getRsaKeys().setPrivateKey(myRsaKeys.getPrivateKey()); // agora sim add a chave privada dps de enviar pro gateway sem
			

			scheduler = Executors.newScheduledThreadPool(1);

			Runnable checkAFK = () -> {
				if(connected) {
					timer--;
					if(timer <= 0) {
						connected = false;
						timer = 60;
					}
				}
			};

			scheduler.scheduleAtFixedRate(checkAFK, 0, 1, TimeUnit.SECONDS);
			
			while(true) {
				while(!connected) {
				
					System.out.println("\tAUTENTICAÇÃO");
					System.out.println("------------------------");
					System.out.println("[1] - Login");
					System.out.println("[2] - Registrar-se");
					System.out.println("------------------------");
					System.out.print("Opção: ");
					int opc = scan.nextInt();
					scan.nextLine();
					System.out.println("------------------------");
					
					switch(opc) {
					case 1:
						connectedUser = login();
						if(connectedUser != null) {
							connected = true;
						} else {
							System.out.println("Login ou senha incorretos. " + ProcessClient.passwordTries + " tentativas restantes.");
							ProcessClient.passwordTries--;
							if(ProcessClient.passwordTries == 0) {
								System.out.println("Sistema bloqueado por 10 segundos.");
								System.out.println("------------------------");
								Thread.sleep(10000);
								ProcessClient.passwordTries = 3;
							}
						}
						break;
					case 2:
						register();
						break;
					default:
						System.out.println("Opção inválida.");
					} 
					
				}

				while(connected) {
					System.out.println("------------------");
					System.out.println("LOJA DE CARROS");
					System.out.println("------------------");
					System.out.println("[1] - Listar carros");
					System.out.println("[2] - Pesquisar carro");
					System.out.println("[3] - Comprar carro");
					System.out.println("[4] - Ver quantidade de carros");
					if(connectedUser.isEmployee()) {
						System.out.println("[5] - Adicionar carro");
						System.out.println("[6] - Alterar carro");
						System.out.println("[7] - Excluir carro");
					}
					System.out.println("[0] - Carros comprados nessa ida à loja");
					System.out.println("------------------");
					System.out.print("Opção: ");
					int opc = 0;
					if(connected)
						opc = scan.nextInt();

					scan.nextLine();
					System.out.println("------------------");
					
					String msgEncrypted;
					String hmac;
					String signature;
					String decryptedMsg;
					String realHMAC;
					boolean validSignature;
					switch(opc) {
					case 1:
						timer = 60;

						System.out.println("\tLISTAGEM");
						System.out.println("------------------");
						System.out.println("[1] - Carros econômicos");
						System.out.println("[2] - Carros intermediários");
						System.out.println("[3] - Carros executivos");
						System.out.println("[4] - Todos os tipos");
						System.out.println("------------------");
						System.out.print("Opção: ");
						int listOpc = 0;
						if(connected)
							listOpc = scan.nextInt();
						scan.nextLine();
						
						List<Car> cars = new ArrayList<Car>();
						
						Message<String> response;
						switch(listOpc) {
							case 1:
							case 2:
							case 3:	
								//cars = gateway.listCars(listOpc);
								timer = 60;
								if(connected) {
									hmac = Hasher.hMac(myKeys.getHMACKey(), String.valueOf(listOpc));
									msgEncrypted = Encrypter.fullEncrypt(myKeys, String.valueOf(listOpc));
									signature = Encrypter.signMessage(myKeys, hmac);
									
									response = gateway.receiveMessage(new Message<String>(1, msgEncrypted, signature, clientNumber));
									
									decryptedMsg = Encrypter.fullDecrypt(myKeys, response.getContent());
									realHMAC = Hasher.hMac(myKeys.getHMACKey(), decryptedMsg);
										
									validSignature = Encrypter.verifySignature(gatewayRSAKeys, realHMAC, response.getMessageSignature());
									
									if(validSignature) {
										String stringCars[] = decryptedMsg.split("¬");
										for(String stringCar : stringCars) {
											if(stringCar != "") {
												String partsCar[] = stringCar.split("°");
												cars.add(new Car(partsCar[0], partsCar[1], Integer.parseInt(partsCar[2]), partsCar[3], Double.parseDouble(partsCar[4])));
											}
										}
									} else {
										System.out.println("Assinatura incorreta. Servidor inválido.");
									}
								}
							break;
							case 4:
								timer = 60;

								//cars = gateway.listCars();
								if(connected) {
									hmac = Hasher.hMac(myKeys.getHMACKey(), "put to pull");
									msgEncrypted = Encrypter.fullEncrypt(myKeys, "put to pull");
									signature = Encrypter.signMessage(myKeys, hmac);
									
									response = gateway.receiveMessage(new Message<String>(111, msgEncrypted, signature, clientNumber));	
									
									decryptedMsg = Encrypter.fullDecrypt(myKeys, response.getContent());
									realHMAC = Hasher.hMac(myKeys.getHMACKey(), decryptedMsg);
										
									validSignature = Encrypter.verifySignature(gatewayRSAKeys, realHMAC, response.getMessageSignature());
									
									if(validSignature) {
										String stringCars[] = decryptedMsg.split("¬");
										for(String stringCar : stringCars) {
											if(stringCar != "") {
												String partsCar[] = stringCar.split("°");
												cars.add(new Car(partsCar[0], partsCar[1], Integer.parseInt(partsCar[2]), partsCar[3], Double.parseDouble(partsCar[4])));
											}
										}
									} else {
										System.out.println("Assinatura incorreta. Servidor inválido.");
									}
								}
							break;
							case 0: break;
							default: System.out.println("Opção inválida."); break;
						}
						
						if(connected) {
							for(Car car : cars) {
								System.out.println("------------------");
								System.out.println("Renavam: " + car.getRenavam());
								System.out.println("Nome: " + car.getName());
								System.out.println("Categoria: " + car.getStringCategory());
								System.out.println("Ano de fabricação: " + car.getManufactureYear());
								System.out.println("Preço: R$" + car.getPrice());
							}
						}
						break;
					case 2:
						timer = 60;

						System.out.println("PESQUISA");
						System.out.println("------------------");
						System.out.println("[1] - Renavam");
						System.out.println("[2] - Nome");
						System.out.println("------------------");
						System.out.print("Opção: ");
						int searchOpc = 0;
						if(connected)
							searchOpc = scan.nextInt();
						scan.nextLine();
						System.out.println("------------------");
						
						switch(searchOpc) {
						case 1:
							timer = 60;

							System.out.print("Digite o renavam: ");
							String renavam = null;
							if(connected)
								renavam = scan.nextLine();
							
							//Car findedCar = gateway.searchCar(renavam);
							
							if(connected) {
								hmac = Hasher.hMac(myKeys.getHMACKey(), renavam);
								msgEncrypted = Encrypter.fullEncrypt(myKeys, renavam);
								signature = Encrypter.signMessage(myKeys, hmac);
								
								response = gateway.receiveMessage(new Message<String>(2, msgEncrypted, signature, clientNumber));
								
								decryptedMsg = Encrypter.fullDecrypt(myKeys, response.getContent());
								realHMAC = Hasher.hMac(myKeys.getHMACKey(), decryptedMsg);
									
								validSignature = Encrypter.verifySignature(gatewayRSAKeys, realHMAC, response.getMessageSignature());
								
								if(validSignature) {
									String partsCar[] = decryptedMsg.split("°");
									Car findedCar = new Car(partsCar[0], partsCar[1], Integer.parseInt(partsCar[2]), partsCar[3], Double.parseDouble(partsCar[4]));
									
									if(!findedCar.getName().equals("null")) {
										System.out.println("------------------");
										System.out.println("Renavam: " + findedCar.getRenavam());
										System.out.println("Nome: " + findedCar.getName());
										System.out.println("Categoria: " + findedCar.getStringCategory());
										System.out.println("Ano de fabricação: " + findedCar.getManufactureYear());
										System.out.println("Preço: R$" + findedCar.getPrice());
									} else {
										System.out.println("Carro não encontrado.");
									}
									
								} else {
									System.out.println("Assinatura incorreta. Servidor inválido.");
								}
							}
							break;
						case 2:
							timer = 60;

							System.out.print("Digite o nome: ");
							String name = null;
							if(connected)
								name = scan.nextLine();
							
							//List<Car> findedsCar = gateway.searchCars(name);
							if(connected) {
								hmac = Hasher.hMac(myKeys.getHMACKey(), name);
								msgEncrypted = Encrypter.fullEncrypt(myKeys, name);
								signature = Encrypter.signMessage(myKeys, hmac);
								
								response = gateway.receiveMessage(new Message<String>(222, msgEncrypted, signature, clientNumber));
								
								decryptedMsg = Encrypter.fullDecrypt(myKeys, response.getContent());
								realHMAC = Hasher.hMac(myKeys.getHMACKey(), decryptedMsg);
									
								validSignature = Encrypter.verifySignature(gatewayRSAKeys, realHMAC, response.getMessageSignature());
								
								if(validSignature) {
									String stringCars[] = decryptedMsg.split("¬");
									List<Car> findedsCar = new ArrayList<Car>();
									for(String stringCar : stringCars) {
										if(stringCar != "") {
											String partsCar[] = stringCar.split("°");
											findedsCar.add(new Car(partsCar[0], partsCar[1], Integer.parseInt(partsCar[2]), partsCar[3], Double.parseDouble(partsCar[4])));
										}
									}
									
									for(Car car : findedsCar) {
										System.out.println("------------------");
										System.out.println("Renavam: " + car.getRenavam());
										System.out.println("Nome: " + car.getName());
										System.out.println("Categoria: " + car.getStringCategory());
										System.out.println("Ano de fabricação: " + car.getManufactureYear());
										System.out.println("Preço: R$" + car.getPrice());
									}
								} else {
									System.out.println("Assinatura incorreta. Servidor inválido.");
								}
							}
							break;
						default:
							System.out.println("Opção inválida.");
						}
						
						break;
					case 3:
						timer = 60;
						if(connected)
							buyCar();
						break;
					case 4:
						timer = 60;

						System.out.println("[1] - Econômico");
						System.out.println("[2] - Intermediário");
						System.out.println("[3] - Executivo");
						System.out.println("[4] - Todas as categorias");
						System.out.println("--------------------");
						System.out.print("Categoria: ");
						int category = 0;
						if(connected) {
							category = scan.nextInt();
						}
						
						//int amount = gateway.getAmount(category);
						hmac = Hasher.hMac(myKeys.getHMACKey(), String.valueOf(category));
						msgEncrypted = Encrypter.fullEncrypt(myKeys, String.valueOf(category));
						signature = Encrypter.signMessage(myKeys, hmac);
						
						response = gateway.receiveMessage(new Message<String>(4, msgEncrypted, signature, clientNumber));
						
						decryptedMsg = Encrypter.fullDecrypt(myKeys, response.getContent());
						realHMAC = Hasher.hMac(myKeys.getHMACKey(), decryptedMsg);
							
						validSignature = Encrypter.verifySignature(gatewayRSAKeys, realHMAC, response.getMessageSignature());
						
						if(validSignature) {
							int amount = Integer.parseInt(decryptedMsg);
							
							System.out.println("--------------------");
							System.out.println("Quantidade de carros na loja: " + amount);
						}
						
						break;
					case 5:
						timer = 60;
						if(connectedUser.isEmployee() && connected) {
							addCar();
						}
						
						break;
					case 6:
						timer = 60;
						if(connectedUser.isEmployee() && connected) {
							editCar();
						}
						
						break;
					case 7:
						timer = 60;
						if(connectedUser.isEmployee() && connected) {
							deleteCar();
						}
						
						break;
					case 0:
						timer = 60;
						System.out.println("--------------------");
						System.out.println("CARROS COMPRADOS");
						for(Car purchased : myCars) {
							System.out.println("--------------------");
							System.out.println("Renavam: " + purchased.getRenavam());
							System.out.println("Nome: " + purchased.getName());
							System.out.println("Categoria: " + purchased.getStringCategory());
							System.out.println("Ano de fabricação: " + purchased.getManufactureYear());
							System.out.println("Preço: R$" + purchased.getPrice());
						}
						
						break;
					case 4545:
						gateway.putToSleep();
						
						break;
					case 5454:
						tryAcessStorage();
						break;
					case 5555:
						tryAcessDatabase();
						break;
					default:
						System.out.println("Opção inválida.");
					
					}
					
				}
			}
			
		} catch (RemoteException | NotBoundException | InvalidKeyException | NoSuchAlgorithmException | UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static void register() throws RemoteException {
		
		System.out.println("\tCADASTRO");
		System.out.println("------------------");
		System.out.println("[1] - Cliente");
		System.out.println("[2] - Funcionário");
		System.out.println("------------------");
		System.out.print("Opção: ");
		int opc = scan.nextInt();
		scan.nextLine();

		System.out.print("Digite o seu nome: ");
		String name = scan.nextLine();
		System.out.print("Digite o seu CPF: ");
		String cpf = scan.nextLine();
		System.out.print("Digite a sua senha: ");
		String password = scan.nextLine();
		System.out.println("------------------------");

		switch (opc) {
		case 1:
			gateway.register(new Client(cpf, password, name)); 
			break;
		case 2:
			gateway.register(new Employee(cpf, password, name));
			break;
		default:
			gateway.register(new Client(cpf, password, name));  // por padrão cria cliente
			System.out.println("Opção inválida. Cadastrado como cliente");
		}
	}
	
	private static User login() throws RemoteException {
		System.out.println("\tLOGIN");
		System.out.println("------------------------");
		System.out.print("CPF: ");
		String cpf = scan.nextLine();
		System.out.print("Senha: ");
		String password = scan.nextLine();
		System.out.println("------------------------");
		
		User connected = gateway.login(cpf, password);
		
		return connected;
		
	}
	
	private static void addCar() throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		System.out.print("Renavam: ");
		String newRenavam = scan.nextLine();
		System.out.print("Nome: ");
		String newName = scan.nextLine();
		System.out.print("Ano de fabricação: ");
		String newYear = scan.nextLine();
		System.out.print("Preço: R$");
		double newPrice = scan.nextDouble();
		scan.nextLine();
		System.out.println("--------------------");
		System.out.println("[1] - Econômico");
		System.out.println("[2] - Intermediário");
		System.out.println("[3] - Executivo");
		System.out.println("--------------------");
		System.out.print("Categoria: ");
		int newCategory = scan.nextInt();
		scan.nextLine();
		
		Car newCar = null;
		switch(newCategory) {
		case 1:
			newCar = new EconomicCar(newRenavam, newName, newCategory, newYear, newPrice);
			//gateway.addCar(new EconomicCar(newRenavam, newName, newCategory, newYear, newPrice));
			break;
		case 2:
			newCar = new IntermediaryCar(newRenavam, newName, newCategory, newYear, newPrice);
			//gateway.addCar(new IntermediaryCar(newRenavam, newName, newCategory, newYear, newPrice));
			break;
		case 3:
			newCar = new ExecutiveCar(newRenavam, newName, newCategory, newYear, newPrice);
			//gateway.addCar(new ExecutiveCar(newRenavam, newName, newCategory, newYear, newPrice));
			break;
		default:
			System.out.println("Opção inválida.");
		}
		
		if(newCategory >= 1 && newCategory <= 3) {
			String hmac = Hasher.hMac(myKeys.getHMACKey(), newCar.toString());
			String msgEncrypted = Encrypter.fullEncrypt(myKeys, newCar.toString());
			String signature = Encrypter.signMessage(myKeys, hmac);
			
			gateway.receiveMessage(new Message<String>(5, msgEncrypted, signature, clientNumber));
			
			System.out.println("--------------------");
			System.out.println("Cadastrado com sucesso.");
		}
	}
	
	private static void editCar() throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		System.out.print("Digite o renavam do carro a ser alterado: ");
		String toEditRenavam = scan.nextLine();
		
		System.out.println("EDIÇÃO");
		System.out.println("------------------");
		System.out.println("[1] - Nome");
		System.out.println("[2] - Categoria");
		System.out.println("[3] - Preço");
		System.out.println("[4] - Ano de fabricação");
		System.out.println("------------------");
		System.out.print("Opção:");
		int editOpc = scan.nextInt();
		scan.nextLine();
		
		String editedName = null, editedYear = null;
		int editedCategory = 0;
		double editedPrice = 0.0;
		switch(editOpc) {
		case 1:
			System.out.print("Novo nome: ");
			editedName = scan.nextLine();
			break;
		case 2:
			System.out.println("[1] - Econômico");
			System.out.println("[2] - Intermediário");
			System.out.println("[3] - Executivo");
			System.out.println("--------------------");
			System.out.print("Nova categoria: ");
			editedCategory = scan.nextInt();
			scan.nextLine();
			break;
		case 3:
			System.out.print("Novo preço: ");
			editedPrice = scan.nextDouble();
			scan.nextLine();
			break;
		case 4:
			System.out.print("Novo ano de fabricação: ");
			editedYear = scan.nextLine();
			break;
		default:
			System.out.println("Opção inválida.");
		}
		
		//gateway.editCar(toEditRenavam, new Car(toEditRenavam, editedName, editedCategory, editedYear, editedPrice));
		Car editedCar = new Car(toEditRenavam, editedName, editedCategory, editedYear, editedPrice);
		String hmac = Hasher.hMac(myKeys.getHMACKey(), editedCar.toString());
		String msgEncrypted = Encrypter.fullEncrypt(myKeys, editedCar.toString());
		String signature = Encrypter.signMessage(myKeys, hmac);
		
		gateway.receiveMessage(new Message<String>(6, msgEncrypted, signature, clientNumber));
		
		System.out.println("--------------------");
		System.out.println("Editado com sucesso.");
	}
	
	private static void deleteCar() throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		System.out.println("EXCLUSÃO");
		System.out.println("------------------");
		System.out.println("[1] - Renavam");
		System.out.println("[2] - Nome");
		System.out.println("------------------");
		System.out.print("Opção:");
		int deleteOpc = scan.nextInt();
		scan.nextLine();

		String hmac;
		String msgEncrypted;
		String signature;
		switch(deleteOpc) {
		case 1:
			System.out.print("Digite o renavam do carro a ser excluído: ");
			String toDeleteRenavam = scan.nextLine();
			
			//gateway.deleteCar(toDeleteRenavam);
			
			hmac = Hasher.hMac(myKeys.getHMACKey(), toDeleteRenavam);
			msgEncrypted = Encrypter.fullEncrypt(myKeys, toDeleteRenavam);
			signature = Encrypter.signMessage(myKeys, hmac);
			
			gateway.receiveMessage(new Message<String>(7, msgEncrypted, signature, clientNumber));
			
			System.out.println("--------------------");
			System.out.println("Carro com renavam " + toDeleteRenavam + " excluído com sucesso.");
			break;
		case 2:
			System.out.print("Digite o nome dos carros a serem excluídos: ");
			String toDeleteName = scan.nextLine();
			
			//gateway.deleteCars(toDeleteName);
			hmac = Hasher.hMac(myKeys.getHMACKey(), toDeleteName);
			msgEncrypted = Encrypter.fullEncrypt(myKeys, toDeleteName);
			signature = Encrypter.signMessage(myKeys, hmac);
			
			gateway.receiveMessage(new Message<String>(777, msgEncrypted, signature, clientNumber));
			
			System.out.println("--------------------");
			System.out.println("Todos os carros " + toDeleteName + " excluídos com sucesso.");
			break;
		default:
			System.out.println("Opção inválida");
		}
		
	}
	
	private static void buyCar() throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		System.out.println("COMPRA DE CARRO");
		System.out.println("------------------");
		System.out.print("Renavam do carro:");
		String renavamToBuy = null;
		if(connected)
			renavamToBuy = scan.nextLine();
		//Car buyCar = gateway.searchCar(renavamToBuy);
		
		if(connected) {
			String hmac = Hasher.hMac(myKeys.getHMACKey(), renavamToBuy);
			String msgEncrypted = Encrypter.fullEncrypt(myKeys, renavamToBuy);
			String signature = Encrypter.signMessage(myKeys, hmac);
			
			Message<String> response = gateway.receiveMessage(new Message<String>(2, msgEncrypted, signature, clientNumber));
			
			String decryptedMsg = Encrypter.fullDecrypt(myKeys, response.getContent());
			String realHMAC = Hasher.hMac(myKeys.getHMACKey(), decryptedMsg);
				

			boolean validSignature = Encrypter.verifySignature(gatewayRSAKeys, realHMAC, response.getMessageSignature());
			
			if(validSignature) {
				String partsCar[] = decryptedMsg.split("°");
				Car buyCar = new Car(partsCar[0], partsCar[1], Integer.parseInt(partsCar[2]), partsCar[3], Double.parseDouble(partsCar[4]));
			
				System.out.println("Nome: " + buyCar.getName());
				System.out.println("Categoria: " + buyCar.getStringCategory());
				System.out.println("Ano de fabricação: " + buyCar.getManufactureYear());
				System.out.println("Preço: R$" + buyCar.getPrice());
				System.out.println("------------------");
				System.out.print("Confirmar compra? (S/N)");
				String confirm = scan.nextLine();
				
				switch(confirm.charAt(0)) {
				case 'Y':
				case 'y':
				case 'S':
				case 's':
					//Car purchased = gateway.buyCar(renavamToBuy);
					response = gateway.receiveMessage(new Message<String>(3, msgEncrypted, signature, clientNumber));
					
					decryptedMsg = Encrypter.fullDecrypt(myKeys, response.getContent());
					realHMAC = Hasher.hMac(myKeys.getHMACKey(), decryptedMsg);
						
					validSignature = Encrypter.verifySignature(gatewayRSAKeys, realHMAC, response.getMessageSignature());
					
					if(validSignature) {
						String partsCar2[] = decryptedMsg.split("°");
						Car purchased = new Car(partsCar2[0], partsCar2[1], Integer.parseInt(partsCar2[2]), partsCar2[3], Double.parseDouble(partsCar2[4]));

						if(!purchased.getName().equals("null")) {
							System.out.println("Compra efetuada com sucesso.");
							System.out.println("Carro: " + purchased.getRenavam());
							myCars.add(purchased);
						} else {
							System.out.println("------------------");
							System.out.println("Já compraram esse carro enquanto você pensava.");
						}	
					} else {
						System.out.println("Assinatura incorreta. Servidor inválido.");
					}
					
					break;
				case 'N':
				case 'n':
					System.out.println("Compra cancelada.");
					break;
				default:
					System.out.println("Opção inválida. Compra cancelada.");
				}
			} else {
				System.out.println("Assinatura incorreta. Servidor inválido.");
			}
		}
	}
	
	private static void tryAcessStorage() throws RemoteException {
		List<Car> cars = storServer.listCars();
		if(cars == null) {
			System.out.println("------------------");
			System.out.println("Você não possui permissão de acesso.");
		}
	}
	
	private static void tryAcessDatabase() throws RemoteException {
		List<Car> cars = dataServer.listCars();
		if(cars == null) {
			System.out.println("------------------");
			System.out.println("Você não possui permissão de acesso.");
		}
	}
	
}
