# Ход решения
  ## Задача 1.
  Даны два файла - один в расширении DMP, второй .enc(просто набор байт).
  По условию в файле DMP есть ключ - набор символов(128бит = 16 символов т.к 1байт = 8 бит = 1 символ)
  По подсказкам узнаем что ключ в файле встречается дважды, так что что в результате должны получить png файл.
  Решение:
  Переберем все возможные 16 символьные значения из файла, отфильтруем только те которые встречаются дважды. Получили около 512 таких значений
  На этом моменте оказалось много вопросов... Каждый из них расшифровывает сообщение, но чтобы получилась нормальная картинка, подходит только один.
  У каждой png картинки есть "волшебные байты" - первые 8 байт всегда одинаковый. Используем же это! Переберем все возможные варианты и отфильтруем только те,
  где в результате первые 8 байт = 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A; Решили, ответ есть и он один.
  Пример кода расчета ECB, остальное можно посмотреть в /main/java/ru/vsu/main/Main:78
  ```
  private static byte[] decryptEcb(byte[] cipherText, SecretKey key) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(cipherText);
    }
    		

  ```
  
  Для первого варианта:
  ![firstTaskRes](https://user-images.githubusercontent.com/15637637/177873730-f0aca6b4-194f-4e07-9250-9e432adc23b0.png)

  ## Задача 2.
  По условию, нужно пройтись по всем пикселям изображения из номера 1 и и найти избыточный код(чексумму), это и будет очередной байт новой картинки jpeg.
  Так же сказано, что должны применить алгоритм CRC8. Только сначала нужно найти правильный полином(и как оказывается не только).
  Решение: 
  Сначала разберемся с CRC8. Это алгоритм расчета чексуммы из байт. Их бывает несколько видов. Именно CRC8 около 4-5 штук. У каждого из них меняются только входные параметры.
  Вот на этом [сайте](https://reveng.sourceforge.io/crc-catalogue/all.htm) можно найти все виды CRC*. Нас интересует только CRC8. 
  Входные параметры для алгоритма выглядят вот так: width=6 poly=0x19 init=0x00 refin=true refout=true xorout=0x00 check=0x26 residue=0x00 name="CRC-6/DARC".
  4 - 5 штук не так много, так что можно перебрать. В подсказке написано что контрольная сумма для слова password = 0xCF;
  Находим правильный алгоритм. Его название = CRC-8/SAE-J1850 с входными параметрами: width=8 poly=0x1d init=0xff refin=false refout=false xorout=0xff check=0x4b residue=0xc4 name="CRC-8/SAE-J1850".
Дальше реализуем этот алгритм, он не сложный выглядит примерно так 
  
```
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
    
```
    
 Сюда подставляем первоначальное значение crc (init=0xff) и так же полином  0x1D. В конце делаем бинарное умножение и xor (xorout=0xff)
  Прогоняем каждый пиксель через через этот алгоритм - на выходе очередной байт. Псевдо код:
    
```
for each pixel:
    blue = pixel.blue();
    green = pixel.green();
    red = pixe.red();
    byteList = new List();
    byte[] rgbByteArray = new byte[]{(byte) blue, (byte) green,   (byte) red, (byte)0x00};
    int newByte = crc8(rgbByteArray);
    byteList.add(newByte);
```	

Конвертируем лист байтов в картинку. Готово! Для первого варианта:
![secondTask](https://user-images.githubusercontent.com/15637637/177873765-4c31b0dc-e4fb-4bbb-a9d8-3781fb894dad.jpeg)

  
  
## Задача 3.
 Опять же по условию сказано что нужно:
 1) Как то сделать ответ с задания 2 чистым (без доп нагрузки) и сделать из него HASH используя MD5
 2) Найти зашифрованное сообщение и расшифровать использую вектор инициализации сделанный с помощью найденного ключа в задании 1.
Решение:
Сначала нароем информацию как выглядит jpeg файл изнутри. Узнаем что в нем есть много маркеров, которые содержат всякие там метаданные и прочую инфу о картинке.
Основные маркеры которые нас интересуют: 0xFFC8 - начало картинки и 0xFFC9 - конец. Пройдемся по массиву байт картинки из второго задания и поймем, что картинка
то заканчивается на 1/4 общего кол-ва байт!(Просто увидели маркер 0xFFC9 раньше конца всех байт). Возьмем отрежем оставшиеся 3/4 и ничего не изменится, картинка останется жива. 
Наши 1/4 файла и есть файл без доп. нагрузки. Дальше было самое сложное, понять где зашифрованное сообщение. Тут уже действовал по интуиции, перебрал все потенциальные возможности.
Оказалось что это первые 16 байт после первого маркера 0xFFC9 (да- да, подсказка.. но слишком много вопросов). Применяем к зашифрованным байтам алгритм AES. 

```

private static byte[] decryptCbc(byte[] cipherText, SecretKey key, IvParameterSpec ivParameterSpec) throws NoSuchPaddingException, NoSuchAlgorithmException,
    InvalidKeyException,
    BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
    
	Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
	cipher.init(Cipher.DECRYPT_MODE, key, ivParameterSpec);
	return cipher.doFinal(cipherText);
	
}

```

Получаем шифр. Готово! (rCv4pyR43dVbe9SH)



```
### Usage по использованию программы:
Зайдите в класс main, в методе Main измените парметры для директории от входных файлов(Положите их в папку resource/data/*). 
Запустите программу - смотрите в консоль
```
