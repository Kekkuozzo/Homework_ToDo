package org.example;

import java.util.Scanner;

public class Utente {
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);;
        System.out.println("inserire email");
        String email = scanner.nextLine();
        System.out.println("la tua mail è " + email);



        System.out.println("inserire password");
        String password = scanner.nextLine();
        System.out.println("la tua password è " + password);
        scanner.close();

}
}
