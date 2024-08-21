package crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class PasswordEncoder {
    
    public static String getHash(String senha, byte[] salt) {
        String senhaGerada = null;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(salt);
            byte[] bytes = md.digest(senha.getBytes());
            senhaGerada = byte2hex(bytes);
        } 
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        
        return senhaGerada;
    }
        
    public static String byte2hex(byte[] bytes) {
        StringBuilder strHex = new StringBuilder();
        for (byte b : bytes) {
            strHex.append(String.format("%02x", b));
        }
        return strHex.toString();
    }
    
    public static byte[] getSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return salt;
    }

}
