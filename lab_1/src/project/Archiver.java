package project;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class Archiver {
    private static final int headerSize = 11;
    private static final int metaDateSize = 6;
    private static byte[] header = new byte[headerSize];
    static {
        //signature
        header[0] = 0b1;
        header[1] = 0b100;
        header[2] = 0b111;
        header[3] = 0b10;
        //version
        header[4] = 0b1;
        //code
        header[5] = 0b0;
        //filter
        header[6] = 0b0;
        //number of files
        header[7] = 0b0;
        header[8] = 0b0;
        header[9] = 0b0;
        header[10] = 0b0;
    }
    public void setVersion(byte version) {
        header[4] = version;
    }
    public void setCode(byte code) {
        header[5] = code;
    }
    public void setFilter(byte filter) {
        header[6] = filter;
    }

    public static boolean archive(ArrayList<File> sourceFiles) {
        boolean isCorrect = true;
        File fileArchive = new File("src\\resources\\d" + sourceFiles.get(0).getName().replaceFirst("[.][^.]+$", ""));
        System.arraycopy(intTo4ByteArray(sourceFiles.size()), 0, header, 7, 4);
        try (FileOutputStream fout = new FileOutputStream(fileArchive)) {
            fout.write(header);

            for (File sourceFile : sourceFiles) {
                if (sourceFile.canRead()) {
                    if (sourceFile.isDirectory()) {
                        isCorrect = writeDirectoryToArchive(sourceFile, fout);
                    }
                    else {
                        isCorrect = writeFileToArchive(sourceFile, fout);
                    }
                }
                else {
                    isCorrect = false;
                }
            }
            return isCorrect;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deArchive(File sourceFile) {
        try (FileInputStream fin = new FileInputStream(sourceFile)) {
            ByteArrayInputStream bais = new ByteArrayInputStream(fin.readAllBytes());
            if (!readHeader(bais)) {
                //return false;
            }
            int numbersFiles = byteArray4ToInt(new byte[] {header[7], header[8], header[9], header[10]});
            for(int i = 0; i < numbersFiles; i++) {
                boolean isDirectory = bais.read() == 1;
                int size = byteArray4ToInt(new byte[]{(byte) bais.read(), (byte) bais.read(), (byte) bais.read(), (byte) bais.read()});
                String path = sourceFile.getParent() + "\\" + getName(bais);
                if (isDirectory) {
                    createDirectory(path, bais, size);
                }
                else {
                    createFile(path, bais, size);
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static byte[] createFileMetadata(File file){
        byte[] fileName = file.getName().getBytes();
        byte[] metaDataFile = new byte[metaDateSize + fileName.length];
        metaDataFile[0] = 0; // is Directory?
        System.arraycopy(intTo4ByteArray(((int) file.length())), 0, metaDataFile, 1, 4); // lengthFile (<2gb)
        metaDataFile[5] = (byte) fileName.length; // size add bites
        System.arraycopy(fileName, 0, metaDataFile, 6, fileName.length); // nameFile

        return metaDataFile;
    }
    private static byte[] createDirectoryMetadata(File directory){
        byte[] fileName = directory.getName().getBytes();
        byte[] metaDataFile = new byte[metaDateSize + fileName.length];
        metaDataFile[0] = (byte) 1; // is Directory?
        System.arraycopy(intTo4ByteArray(Objects.requireNonNull(directory.listFiles()).length), 0, metaDataFile, 1, 4); // lengthFile (<2gb)
        metaDataFile[5] = (byte) fileName.length; // size add bites
        System.arraycopy(fileName, 0, metaDataFile, 6, fileName.length); // nameFile

        return metaDataFile;
    }
    private static boolean writeFileToArchive(File file, FileOutputStream fout) {
        try (FileInputStream fin = new FileInputStream(file)) {
            byte[] metaDataFile = createFileMetadata(file);
            fout.write(metaDataFile);
            fin.transferTo(fout);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    private static boolean writeDirectoryToArchive(File directory, FileOutputStream fout) {
        byte[] metaDataFile = createDirectoryMetadata(directory);
        boolean isCorrect = true;
        try {
            fout.write(metaDataFile);
            for(File file : Objects.requireNonNull(directory.listFiles())) {
                if (file.isDirectory()) {
                    isCorrect = writeDirectoryToArchive(file, fout);
                }
                else {
                    isCorrect = writeFileToArchive(file, fout);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return isCorrect;
    }


    private static boolean readHeader(ByteArrayInputStream bais) throws IOException {
        if (!checkSignature(bais)){
            return false;
        }
        bais.read(header, 4, headerSize - 4);
        return true;
    }
    private static boolean checkSignature(ByteArrayInputStream bais) throws IOException {
        byte[] signature = new byte[4];
        bais.read(signature);
        return Arrays.equals(signature, 0, 3, header, 0, 3);
    }
    private static String getName(ByteArrayInputStream bais) throws IOException {
        int fileNameSize = bais.read();
        byte[] fileNameBytes = new byte[fileNameSize];
        bais.read(fileNameBytes);
        return new String(fileNameBytes, StandardCharsets.UTF_8);
    }
    private static boolean createFile(String filePath, ByteArrayInputStream bais, int fileLength) throws IOException {
        File file = new File(filePath);
        file.createNewFile();
        try (FileOutputStream fout = new FileOutputStream(file)) {
            fout.write(bais.readNBytes(fileLength));
            return true;
        }catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    private static boolean createDirectory(String directoryPath, ByteArrayInputStream bais, int numberFiles) throws IOException {
        File file = new File(directoryPath);
        file.mkdirs();
        for(int i = 0; i < numberFiles; i++) {
            boolean isDirectory = bais.read() == 1;
            int size = byteArray4ToInt(new byte[]{(byte) bais.read(), (byte) bais.read(), (byte) bais.read(), (byte) bais.read()});
            String path = file.getPath() + "\\" + getName(bais);
            if (isDirectory) {
                createDirectory(path, bais, size);
            }
            else {
                createFile(path, bais, size);
            }
        }
        return true;
    }

    private static byte[] intTo4ByteArray(int number) {
        byte[] bytesNumber = new byte[4];
        bytesNumber[3] = (byte) (number & 0xFF); // 1-4 fileLength
        bytesNumber[2] = (byte) (number >> 8 & 0xFF);
        bytesNumber[1] = (byte) (number >> 16 & 0xFF);
        bytesNumber[0] = (byte) (number >> 24 & 0xFF);
        return bytesNumber;
    }
    private static int byteArray4ToInt(byte[] bytesNumber) {
        return  (bytesNumber[0] << 24) + ((bytesNumber[1] << 16) & 0xFFFFFF)   + ((bytesNumber[2] << 8) & 0xFFFF)  + (bytesNumber[3] & 0xFF);
    }
}
