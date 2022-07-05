import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;
import java.util.Stack;

public class Main {
    private static final String TAG = "TAG";
    private static final String ans = "LetoCTF{s0m3_ez_Andr01d_r3v}";
    private static final String last = "_r3v}";
    private static long next = -1638144296173776243L;
    private static long s = -1655835832096201751L;

    public static void main(String[] args) throws NoSuchAlgorithmException {
        String l = Long.toBinaryString(next);
        String k = Long.toBinaryString(s);
        StringBuilder b = new StringBuilder();
        for (int p = 0; p < l.length(); p++){
            if(l.charAt(p) == k.charAt(p)){
                b.append('0');
            }else{
                b.append('1');
            }
        }
        long j = Long.parseLong(b.toString(), 2);
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < 7; i++){
            sb.append((char)(j % 256));
            j >>= 8;
        }
        sb.reverse();
        System.out.println(sb.toString());
    }

    private static boolean fourthCheck(String str) {
        long nextLong = new Random(865790124).nextLong();
        long j = 0;
        for (byte b : str.getBytes()) {
            j = (j << 8) + ((long) (b & 255));
        }
        long j2 = nextLong ^ j;
        System.out.println(j2);
        System.out.println(-1655835832096201751L);
        if (j2 == -1655835832096201751L) {
            return true;
        }
        return false;
    }

    private static boolean firstCheck(String str) {
        byte[] bytes = "Hackerdom".getBytes();
        byte[] bArr = {4, 4, 23, 4, 38, 38, 34, 20, 30};
        byte[] bytes2 = str.getBytes();
        for (int i = 0; i < bytes.length; i++) {
            for(byte k = 0; k != (byte)255; k++){
                byte b = (byte) (bytes[i] ^ k);
                if(b == bArr[i]){
                    System.out.println(k);
                    break;
                }
            }
            bytes[i] = (byte) (bytes[i] ^ bytes2[i]);
        }
        return Arrays.equals(bArr, bytes);
    }

    private static boolean secondCheck(String str) {
        char[] charArray = "0n5b".toCharArray();
        char[] charArray2 = str.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            System.out.print((char)(charArray[i] - i));
            charArray2[i] = (char) (charArray2[i] + i);
        }
        return Arrays.equals(charArray, charArray2);
    }

    private static boolean thirdCheck(String str) throws NoSuchAlgorithmException {
        MessageDigest instance = MessageDigest.getInstance("MD5");
        instance.update(str.getBytes(StandardCharsets.UTF_8));
        return Arrays.equals(instance.digest(), new byte[]{-78, 45, -66, 74, -120, 86, -12, -104, -20, 125, 14, 102, -65, -6, 105, -53});
    }



    private static boolean fifthCheck(String str) {
        char[] cArr = {'3', 'v', '}', '_', 'r'};
        for (int i = 0; i < 5; i++) {
            if (str.charAt(i) != cArr[(i + 3) % 5]) {
                return false;
            }
        }
        return true;
    }
}
