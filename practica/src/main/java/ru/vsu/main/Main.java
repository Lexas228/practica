package ru.vsu.main;

import com.drew.imaging.ImageProcessingException;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class Main{
    private static final int[] pngBytes = new int[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final int[] jpegBytes = new int[]{0xFF,0xD8, 0xFF, 0xE0};

    public static void main(String[] args) throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, URISyntaxException {
        byte[] dumpInBytes = FileUtil.readData("data/dump_001.DMP");
        byte[] encrDataInBytes = FileUtil.readData("data/encr_001");
        Pair<byte[], byte[]> taskOne = solveTaskOne(dumpInBytes, encrDataInBytes);
        if (taskOne == null) {
            throw new IllegalArgumentException("Ответ на первое задание не был найден, проверьте корректность указанного пути");
        }else{
            System.out.println("Первое задание решено! Ключ = "  + new String(taskOne.getFirst()));
            FileUtil.writeData("firstTaskRes.png", taskOne.getSecond());
            System.out.println("Картинка записана в ./firstTaskRes.png");
        }
        byte[] dataFromTaskOne = taskOne.getSecond();
        byte[] taskTwo = solveTaskTwo(dataFromTaskOne);
        System.out.println("Второе задание решено!");
        FileUtil.writeData("secondTask.jpeg", taskTwo);
        System.out.println("Картнка записана в ./secondTask.jpeg");
        byte[] keyFromTaskOne = taskOne.getFirst();
        byte[] taskThreeRes = solveTaskThree(keyFromTaskOne, taskTwo);
        System.out.println("Ответ на третье задание найдено! Ключ = " + new String(taskThreeRes));
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

    private static byte[] getByteArrayFromList(List<Byte> byteList){
        byte[] bytes = new byte[byteList.size()];
        for(int i = 0; i < bytes.length; i++){
            bytes[i] = byteList.get(i);
        }
        return bytes;
    }


    private static Pair<byte[], byte[]> solveTaskOne(byte[] dumpInBytes, byte[] encrDataInBytes) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        List<byte[]> keys = getKey(dumpInBytes);
        System.out.println("Общее кол-во ключей: " + keys.size());
        List<byte[]> keysAfterFilter = filterKeys(keys, 1);
        System.out.println("Общее кол-во ключей после фильтрации: " + keysAfterFilter.size());
        int attempt = 1;
        while (keysAfterFilter.isEmpty()){
            System.out.println("Попытка увеличить максимальное значение встречающихся ключей #" + 1);
            keysAfterFilter = filterKeys(keys, 1+attempt);
            attempt++;
            if(attempt > 16){
                throw new IllegalStateException("Ключ не был найден");
            }
        }

        for(byte[] key : keysAfterFilter){
            byte[] ans = decryptEcb(encrDataInBytes, createKeySpec(key));
            boolean good = true;
            for(int i = 0; i < pngBytes.length; i++){
                if(Byte.toUnsignedInt(ans[i]) != pngBytes[i]){
                    good = false;
                    break;
                }
            }
            if(good) {
                return new Pair<>(key, ans);
            }
        }
        return null;
    }


    private static List<byte[]> filterKeys(List<byte[]> bytes, int maxValue){
       return bytes.stream().filter(byteArray -> {
            Map<Byte, Integer> byteCount = new HashMap<>();
            int max = -1;
            for(byte bt : byteArray){
                int next = byteCount.getOrDefault(bt, 0) + 1;
                byteCount.put(bt, next);
                max = Math.max(max, next);
            }
            return max <= maxValue;
        }).collect(Collectors.toList());
    }



    private static byte[] solveTaskThree(byte[] firstTaskKey, byte[] secondTaskRes) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        List<Byte> cleanTaskTwo = new ArrayList<>();
        int s = 0;
        while(s < secondTaskRes.length){
            if(Byte.toUnsignedInt(secondTaskRes[s]) == 0xFF){
                if(Byte.toUnsignedInt(secondTaskRes[s+1]) == 0xD9){
                    cleanTaskTwo.add(secondTaskRes[s]);
                    cleanTaskTwo.add(secondTaskRes[s+1]);
                    s+=2;
                    break;
                }
            }
            cleanTaskTwo.add(secondTaskRes[s]);
            s++;
        }
        byte[] cleanTaskTwoBytes = getByteArrayFromList(cleanTaskTwo);
        byte[] chiperText= new byte[16];
        int k = 0;
        for(int i = s; i < s + 16; i++){
            chiperText[k] = secondTaskRes[i];
            k++;
        }
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(cleanTaskTwoBytes);

        return decryptCbc(chiperText, new SecretKeySpec(hash, "AES"), new IvParameterSpec(firstTaskKey));
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
        return answer;
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
        //PKCS5PADDING
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
}

