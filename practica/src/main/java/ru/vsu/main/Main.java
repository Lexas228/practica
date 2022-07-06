package ru.vsu.main;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Main{
    private static final String CipherMode = "AES/ECB/NoPadding";
    private static final int[] pngBytes = new int[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final int[] jpegBytes = new int[]{0xFF,0xD8, 0xFF, 0xD9};

    public static void main(String[] args) throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        byte[] bytes = solveTaskOne("data/dump_002.DMP", "data/encr_002");
        if (bytes == null) {
            throw new IllegalArgumentException("Ответ на первое задание не был найден, проверьте корректность указанного пути");
        }
        System.out.println(Integer.toHexString(bytes[0]));

        InputStream is = new ByteArrayInputStream(bytes);
        BufferedImage newBi = ImageIO.read(is);
        List<Integer> byteList = new ArrayList<>();
        for(int i = 0; i < newBi.getWidth(); i++){
            for(int j = 0; j < newBi.getHeight(); j++){
                Color c = new Color(newBi.getRGB(i, j), true);
                //System.out.println( newBi.getRGB(i, j) + " " + Integer.toHexString(newBi.getRGB(i, j) & 0xFF));
                int red = c.getRed();
                int green = c.getGreen();
                int blue = c.getBlue();
                int alpha = c.getAlpha();
                byte RGB = (byte)((red << 5) | (green << 2) | blue);
                System.out.println(Integer.toHexString(red) + " " + Integer.toHexString(green) + " " + Integer.toHexString(blue) + " " + Integer.toHexString(alpha));

                //byte[] b = new byte[]{(byte) red, (byte) green, (byte) blue, (byte) alpha};
                //byteList.add(crc8(RGB));


            }
            return;
        }
        byte[] answer = new byte[byteList.size()];
        for(int i = 0; i < answer.length; i++){
            answer[i] = byteList.get(i).byteValue();
        }
        System.out.println((answer[0]) + " " + jpegBytes[0] + " " + pngBytes[0]);
        System.out.println((answer[1]) + " " + jpegBytes[1] + " " + pngBytes[1]);



        /*ByteArrayInputStream bis = new ByteArrayInputStream(answer);
        BufferedImage bImage2 = ImageIO.read(bis);
        ImageIO.write(bImage2, "jpg", new File("./output.jpg"));*/

    }

    private static List<byte[]> getKey(byte[] data){
        Map<BitSet, Integer> allCount = new HashMap<>();

        for(int i = 0; i < data.length; i++){
            byte[] b = new byte[16];

            for(int j = i, s = 0; j < i + 16 && j < data.length; j++, s++){
                b[s] = data[j];
            }
            BitSet bs = BitSet.valueOf(b);
            if(bs.size() == 128 && bs.toByteArray().length == 16){
                allCount.put(bs, allCount.getOrDefault(bs, 0) +1);
            }
        }
        return allCount.keySet().stream().filter(key -> allCount.get(key).equals(2)).map(BitSet::toByteArray).collect(Collectors.toList());
    }


    private static byte[] solveTaskOne(String pathToDump, String pathToData) throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {

        byte[] dumpInBytes = readData(pathToDump);
        byte[] encrDataInBytes = readData(pathToData);
        List<byte[]> keys = getKey(dumpInBytes);

        for(byte[] key : keys){
            byte[] ans = decrypt(encrDataInBytes, createKeySpec(key));
            boolean good = true;
            for(int i = 0; i < pngBytes.length; i++){
                if(Byte.toUnsignedInt(ans[i]) != pngBytes[i]){
                    good = false;
                    break;
                }
            }
            if(good) {
                System.out.println("Первое задание решено! Ключ = " + new String(key));
                return ans;
            }
        }
        return null;
    }

    
    private static byte[] readData(String path) throws IOException {
        ClassLoader classLoader = Main.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(path);
        byte[] bytes = new byte[Objects.requireNonNull(inputStream).available()];
        int read = inputStream.read(bytes);
        return bytes;
    }

    public static int[][] getImageToPixels(BufferedImage bufferedImage) {
        if (bufferedImage == null) {
            throw new IllegalArgumentException();
        }
        int h = bufferedImage.getHeight();
        int w = bufferedImage.getWidth();
        int[][] pixels = new int[h][w];
        for (int i = 0; i < h; i++) {
            bufferedImage.getRGB(0, i, w, 1, pixels[i], 0, w);
        }
        return pixels;
    }


    private static byte[] decrypt(byte[] cipherText, SecretKey key) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(CipherMode);
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(cipherText);
    }

    private static SecretKeySpec createKeySpec(byte[] key){
        return new SecretKeySpec(key, "AES");
    }

    public static int crc8(byte[] data) {
        int crc = 0xff;
        for (byte datum : data) {
            crc = (crc ^ datum);
            for (int j = 0; j < 8; j++) {
                crc =  (crc & 0x80) != 0 ? ((crc<<1) ^ 0x1D) :  (crc << 1);
            }
        }
        crc &=  0xFF;
        crc ^=  0xFF;
        return crc;
    }

    public static int crc8(byte data) {
        return crc8(new byte[]{data});
    }

    public static int crc8(int data) {
        return crc8(new byte[]{(byte)data});
    }
}

