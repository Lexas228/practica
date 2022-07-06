package ru.vsu.main;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
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
        if(bytes == null){
            throw new IllegalArgumentException("Ответ на первое задание не был найден, проверьте корректность указанного пути");
        }

        InputStream is = new ByteArrayInputStream(bytes);
        BufferedImage newBi = ImageIO.read(is);
        byte[] pixels = getImageToPixels(newBi);
        System.out.println(pixels.length);

        String str = "password";

        byte[] passWordBytes = str.getBytes(StandardCharsets.UTF_8);

        List<Byte> allGoodInitial = new ArrayList<>();
        List<Byte> allGoodPolynoms = new ArrayList<>();

        for(byte initial = -127; initial < 127; initial++){
            boolean good = false;
            for(byte polyminal = -127; polyminal < 127; polyminal++){
                int ans = crc8(passWordBytes, initial, polyminal);
                if(ans == 0xCF){
                    good = true;

                }
                allGoodPolynoms.add(polyminal);
            }
            allGoodInitial.add(initial);
            //if(good)

        }

        Byte ansPol = null;
        Byte ansInit = null;
        for(Byte polByte : allGoodPolynoms){
            for(Byte initByte : allGoodInitial){
                boolean good = true;
                for(int i = 0; i < 2; i++){
                    int ans = crc8(pixels[i], initByte, polByte);
                    //System.out.println(ans);
                    if(ans != jpegBytes[i]) {
                        good = false;
                        break;
                    }
                }
                if(good){
                    ansPol = polByte;
                    ansInit = initByte;
                }
            }
        }

        if(ansPol == null){
            throw new IllegalArgumentException("Полином не был найден! Что то не так");
        }
        System.out.println("Полином " + ansPol);
        System.out.println("Инит " + ansInit);

        byte[] data = new byte[pixels.length];

        int i = 0;
        for(byte b : pixels){
            byte ans = (byte)crc8(b, ansInit, ansPol);
            data[i] = ans;
            i++;
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        BufferedImage bImage2 = ImageIO.read(bis);
        ImageIO.write(bImage2, "jpeg", new File("./output1.jpeg") );
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

    public static byte[] getImageToPixels(BufferedImage bufferedImage) {
        if (bufferedImage == null) {
            throw new IllegalArgumentException();
        }
        return ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
    /*    int h = bufferedImage.getHeight();
        int w = bufferedImage.getWidth();
        int[][] pixels = new int[h][w];
        for (int i = 0; i < h; i++) {
            bufferedImage.getRGB(0, i, w, 1, pixels[i], 0, w);
        }
        return pixels;*/
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


    public static int crc8(byte[] data, byte crcInit, byte poly) {
        int _crc = crcInit;
        for (int value : data) {
            _crc = _crc ^ value;
            for (int i = 8; i > 0; i--) {
                if ((_crc & (1 << 7)) > 0) {
                    _crc <<= 1;
                    _crc ^= poly;
                } else {
                    _crc <<= 1;
                }
            }
        }
       return  (_crc & 0xff);
    }

    public static int crc8(byte data, byte crcInit, byte poly) {
        int _crc = crcInit ^ data;
        for (int i = 8; i > 0; i--) {
            if ((_crc & (1 << 7)) > 0) {
                _crc <<= 1;
                _crc ^= poly;
            } else {
                _crc <<= 1;
            }
        }
        return  (_crc & 0xff);
    }

    public static int uint(byte v) {
        return v & 0xFF;
    }
}
