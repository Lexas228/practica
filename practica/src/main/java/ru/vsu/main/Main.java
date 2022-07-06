package ru.vsu.main;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Main{
    private static final int[] pngBytes = new int[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final int[] jpegBytes = new int[]{0xFF,0xD8, 0xFF, 0xE0};

    public static void main(String[] args) throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        Pair<byte[], byte[]> taskOne = solveTaskOne("data/dump_002.DMP", "data/encr_002");
        if (taskOne == null) {
            throw new IllegalArgumentException("Ответ на первое задание не был найден, проверьте корректность указанного пути");
        }
        byte[] dataFromTaskOne = taskOne.getSecond();

        byte[] taskTwo = solveTaskTwo(dataFromTaskOne);


        int k = 0;
        for(int i = 0; i < taskTwo.length; i++){
            if(Byte.toUnsignedInt(taskTwo[i]) == 0xFF && i < taskTwo.length-1 && Byte.toUnsignedInt(taskTwo[i+1]) == 0xD8){
                k++;
            }
        }
        System.out.println(k);
/*
        byte[] clean = new byte[taskTwo.length - 4];

        System.arraycopy(taskTwo, 4, clean, 0, taskTwo.length - 4);

        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(clean);
        byte[] keyFromTaskOne = taskOne.getFirst();
        SecretKeySpec keySpec = createKeySpec(hash);
        IvParameterSpec iv = new IvParameterSpec(keyFromTaskOne);
        byte[] ans = decryptCbc(taskTwo, keySpec, iv);
        System.out.println(new String(ans));*/


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


    private static Pair<byte[], byte[]> solveTaskOne(String pathToDump, String pathToData) throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {

        byte[] dumpInBytes = readData(pathToDump);
        byte[] encrDataInBytes = readData(pathToData);
        List<byte[]> keys = getKey(dumpInBytes);

        for(byte[] key : keys){
            byte[] ans = decryptEcb(encrDataInBytes, createKeySpec(key));
            boolean good = true;
            for(int i = 0; i < pngBytes.length; i++){
                if(Byte.toUnsignedInt(ans[i]) != pngBytes[i]){
                    good = false;
                    break;
                }
            }
            if(good) {
                System.out.println("Первое задание решено! Ключ = " + new String(key));
                return new Pair<>(key, ans);
            }
        }
        return null;
    }

    private static byte[] solveTaskTwo(byte[] arrayOfTaskOne) throws IOException {
        InputStream is = new ByteArrayInputStream(arrayOfTaskOne);
        BufferedImage newBi = ImageIO.read(is);
        List<Integer> byteList = new ArrayList<>();
        for(int i = 0; i < newBi.getHeight(); i++){
            for(int j = 0; j < newBi.getWidth(); j++){
                Color c = new Color(newBi.getRGB(j, i), false);;
                int red = c.getRed();
                int green = c.getGreen();
                int blue = c.getBlue();
                byte[] be = new byte[]{(byte) blue, (byte) green,   (byte) red, (byte)0x00};
                int i1 = crc8(be);
                byteList.add(i1);
            }
        }
        byte[] answer = new byte[byteList.size()];
        for(int i = 0; i < answer.length; i++){
            answer[i] = byteList.get(i).byteValue();
        }
        for(int i = 0; i < 4; i++){
            if(Byte.toUnsignedInt(answer[i]) != jpegBytes[i]){
                throw new IllegalArgumentException("Ошибка во втором здании, формат не jpeg!");
            }
        }
        System.out.println("Задание два решено успешно!");
        return answer;
    }

    
    private static byte[] readData(String path) throws IOException {
        ClassLoader classLoader = Main.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(path);
        byte[] bytes = new byte[Objects.requireNonNull(inputStream).available()];
        int read = inputStream.read(bytes);
        return bytes;
    }


    private static byte[] decryptEcb(byte[] cipherText, SecretKey key) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(cipherText);
    }

    private static byte[] decryptCbc(byte[] cipherText, SecretKey key, IvParameterSpec ivParameterSpec) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {

        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);
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

