package amichno.db_perf_app;

import java.util.Random;

public class UserDataGenerator {
    private static final String[] firstNames = {
            "Mateusz", "Daniel", "Rafał", "Denis", "Jacek", "Bartłomiej",
            "Magdalena", "Krystyna", "Roksana", "Dagmara", "Aleksandra", "Justyna", "Gabriela", "Ilona"
    };
    private static final String[] lastNames = {
            "Baran", "Kaczmarczyk", "Pietrzak", "Krupa", "Kubiak", "Mazur", "Michalak",
            "Sikora", "Marciniak", "Kołodziej", "Błaszczyk", "Szulc", "Pawlak", "Cieślak"
    };
    private static final String[] domains = {
            "@gmail.com", "@yahoo.com", "@hotmail.com", "@onet.pl", "@o2.pl", "@gov.pl", "hogwarts.edu"
    };
    private static final String[] emailLogins = {
            "mati", "deny", "cukierek", "jacek", "koza", "kozlowski", "roksi", "bolek",
            "jarzyna", "rajmund", "pleksa28", "harry", "hermiona"
    };

    private static final Random rand = new Random();

    private static String getRandomElement(String[] array) {
        return array[rand.nextInt(array.length)];
    }

    public static String getName() {
        return getRandomElement(firstNames) + " " + getRandomElement(lastNames);
    }

    public static String getEmail() {
        return getRandomElement(emailLogins) + getRandomElement(domains);
    }

    public static int getAge() {
        return rand.nextInt(64) + 12;
    }

    public static String getPhone() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < 9; i++)
            sb.append(rand.nextInt(10));
        return sb.toString();
    }
}
