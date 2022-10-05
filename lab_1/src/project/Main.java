package project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ArrayList<File> files = new ArrayList<>();
        files.add(new File("src\\test\\testDir1"));
        files.add(new File("src\\test\\test2.txt"));
        files.add(new File("src\\test\\text1.txt"));
        System.out.println("1 — archive; 2 — deArchive");
        try {
            switch (scanner.nextInt()) {
                case 1 -> Archiver.archive(files);
                case 2 -> Archiver.deArchive(new File("src\\resources\\dtestDir1"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
