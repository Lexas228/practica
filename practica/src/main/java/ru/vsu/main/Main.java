package ru.vsu.main;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    private static final String CipherMode = "AES/ECB/PKCS5Padding";
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        ClassLoader classLoader = Main.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("data/dump_002.DMP");
        byte[] bytes = new byte[inputStream.available()];
        inputStream.read(bytes);
        Map<String, Integer> all = new HashMap<>();


        for(int i = 0; i < bytes.length; i++){
            byte[] k = new byte[16];
            int s = 0;
            for(int j = i; j < i+16 && j < bytes.length; j++){
                k[s] = bytes[j];
                s++;
            }

            if(s == 16){
                String l = new String(k);
                boolean good = true;
                for(int p = 0; p < l.length(); p++){
                    if(!Character.isDigit(l.charAt(p))){
                        good = false;
                        break;
                    }
                }
                if(good) {
                    int r = all.getOrDefault(new String(k), 0) + 1;
                    all.put(new String(k), r);
                }
            }

        }
        all.keySet().forEach(key -> {
            if(all.get(key) == 2)
                System.out.println(key);
        });
       // map.keySet().stream().filter(key -> map.get(key).equals(2)).forEach(System.out::println);
        /*String data = readFromInputStream(inputStream);
        System.out.println(generateKey(128).getEncoded().length);*/
        //List<String> key = getKey(data);
        //key.forEach(System.out::println);


/*

        InputStream streamOfCipherText = classLoader.getResourceAsStream("data/encr_002");
        byte[] bytes = new byte[streamOfCipherText.available()];
        streamOfCipherText.read(bytes);
        int i = 0;
        for(String k : key){

            try {
                byte[] ans = decrypt(bytes, createKeySpec(k));
                DataBuffer buffer = new DataBufferByte(ans, ans.length);
                int width = 200;
                int height = 200;

                WritableRaster raster = Raster.createInterleavedRaster(buffer, width, height, 3 * width, 3, new int[] {0, 1, 2}, (Point)null);
                ColorModel cm = new ComponentColorModel(ColorModel.getRGBdefault().getColorSpace(), false, true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
                BufferedImage image = new BufferedImage(cm, raster, true, null);

                ImageIO.write(image, "png", new File("image" + i + ".png"));
            }catch (Throwable ignored){
                System.out.println(ignored);
            }
            i++;
        }

*/

    }




    private static String readFromInputStream(InputStream inputStream) throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br
                     = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line);
            }
        }
        return resultStringBuilder.toString();
    }

    private static List<String> getKey(String data){
        Map<String, Integer> allCount = new HashMap<>();

        for(int i = 0; i < data.length(); i++){
            StringBuilder builder = new StringBuilder();
            for(int j = i; j < i + 15 && j < data.length(); j++){
                char ch  = data.charAt(j);
                builder.append(ch);

            }
            if(builder.toString().length() == 15){
                int count = allCount.getOrDefault(builder.toString(), 0) +1;
                allCount.put(builder.toString(), count);
            }
        }
        return allCount.keySet().stream().filter(key -> allCount.get(key).equals(2)).collect(Collectors.toList());
    }


    private static byte[] decrypt(byte[] cipherText, SecretKey key) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(cipherText);
    }

    private static SecretKeySpec createKeySpec(String key){
        return new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
    }

    private static byte[] hex2byte(String inputString) {
        if (inputString == null || inputString.length() < 2) {
            return new byte[0];
        }
        inputString = inputString.toLowerCase();
        int l = inputString.length() / 2;
        byte[] result = new byte[l];
        for (int i = 0; i < l; ++i) {
            String tmp = inputString.substring(2 * i, 2 * i + 2);
            result[i] = (byte) (Integer.parseInt(tmp, 16) & 0xFF);
        }
        return result;
    }

    public static byte[] getFile() {

        File f = new File("/home/bridgeit/Desktop/Olympics.jpg");
        InputStream is = null;
        try {
            is = new FileInputStream(f);
        } catch (FileNotFoundException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        byte[] content = null;
        try {
            content = new byte[is.available()];
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            is.read(content);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return content;
    }

    public static SecretKey generateKey(int n) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(n);
        SecretKey key = keyGenerator.generateKey();
        return key;
    }
}
