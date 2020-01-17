package edu.purdue.jtk;
// See https://peterkovesi.com/projects/colourmaps/ and https://arxiv.org/abs/1509.03700

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

// import static javafx.scene.paint.Color.color;

/**
 * Rainbow -- create and use a perceptually uniform array of colors.
 *
 * See https://peterkovesi.com/projects/colourmaps/ and https://arxiv.org/abs/1509.03700.
 */
public class Rainbow {
    public static int get(float f) {
        if (f < 0 || f > 1.0)
            return 0; // Use black for data out of range (typically, NaN from headband)
        int i = (int) (f * 255);
        return rainbow[i];
    }

    public static void main(String[] args) throws IOException {
        ArrayList<Integer> rainbow = readRainbow("data/CET-R2.csv");
        printDeclaration(rainbow);
    }

    private static void printDeclaration(ArrayList<Integer> rainbow) {
        System.out.println("private static int[] rainbow = {");
        int c = 0;
        for (int i = 0; i < rainbow.size(); i++) {
            if (c == 0) System.out.format("    ");
            System.out.format("0x%X, ", rainbow.get(i));
            if (c++ > 8) {
                System.out.format("\n");
                c = 0;
            }
        }
        System.out.println("\n    };");
    }

    static ArrayList<Integer> readRainbow(String file) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        ArrayList<Integer> rainbow = new ArrayList<>();

        while ((line = br.readLine()) != null) {
            String[] values = line.split(",");
            int red = Integer.parseInt(values[0]);
            int green = Integer.parseInt(values[1]);
            int blue = Integer.parseInt(values[2]);
            int color = 0xFF << 24 | red << 16 | green << 8 | blue;
            rainbow.add(color);
        }
        return rainbow;
    }

    private static int[] rainbow = {
        0xFF0034F5, 0xFF0037F3, 0xFF003AF0, 0xFF003CED, 0xFF003FEA, 0xFF0041E7, 0xFF0044E5, 0xFF0046E2, 0xFF0048DF, 0xFF004ADC,
        0xFF004DD9, 0xFF004FD6, 0xFF0051D4, 0xFF0053D1, 0xFF0055CE, 0xFF0057CB, 0xFF0059C9, 0xFF005AC6, 0xFF005CC3, 0xFF005EC0,
        0xFF0060BD, 0xFF0062BB, 0xFF0063B8, 0xFF0065B5, 0xFF0067B2, 0xFF0068B0, 0xFF006AAD, 0xFF006CAA, 0xFF006DA7, 0xFF006FA5,
        0xFF0070A2, 0xFF0071A0, 0xFF00739D, 0xFF00749A, 0xFF007598, 0xFF017695, 0xFF087793, 0xFF0E7891, 0xFF147A8E, 0xFF187B8C,
        0xFF1C7C89, 0xFF207D87, 0xFF237E85, 0xFF267F82, 0xFF298080, 0xFF2B817E, 0xFF2D827B, 0xFF2F8379, 0xFF318476, 0xFF338574,
        0xFF348672, 0xFF35876F, 0xFF37886D, 0xFF38896A, 0xFF398A68, 0xFF3A8B65, 0xFF3B8C63, 0xFF3B8D60, 0xFF3C8E5E, 0xFF3D8F5B,
        0xFF3D9058, 0xFF3E9156, 0xFF3E9253, 0xFF3E9350, 0xFF3F944E, 0xFF3F954B, 0xFF3F9648, 0xFF3F9845, 0xFF3F9942, 0xFF3F9A3F,
        0xFF3F9B3C, 0xFF3F9C39, 0xFF3F9D35, 0xFF3F9E32, 0xFF3F9F2F, 0xFF3FA02B, 0xFF3FA128, 0xFF40A224, 0xFF40A321, 0xFF41A41E,
        0xFF42A51B, 0xFF44A618, 0xFF46A616, 0xFF48A714, 0xFF4AA812, 0xFF4DA910, 0xFF4FA910, 0xFF52AA0F, 0xFF55AB0F, 0xFF57AB0F,
        0xFF5AAC0F, 0xFF5DAC0F, 0xFF60AD10, 0xFF62AE10, 0xFF65AE11, 0xFF68AF11, 0xFF6AAF12, 0xFF6DB012, 0xFF6FB112, 0xFF72B113,
        0xFF74B213, 0xFF77B214, 0xFF79B314, 0xFF7CB415, 0xFF7EB415, 0xFF81B516, 0xFF83B516, 0xFF85B617, 0xFF88B617, 0xFF8AB718,
        0xFF8CB718, 0xFF8FB819, 0xFF91B919, 0xFF93B91A, 0xFF96BA1A, 0xFF98BA1A, 0xFF9ABB1B, 0xFF9CBB1B, 0xFF9FBC1C, 0xFFA1BC1C,
        0xFFA3BD1D, 0xFFA5BE1D, 0xFFA8BE1E, 0xFFAABF1E, 0xFFACBF1F, 0xFFAEC01F, 0xFFB0C01F, 0xFFB3C120, 0xFFB5C120, 0xFFB7C221,
        0xFFB9C221, 0xFFBBC322, 0xFFBEC322, 0xFFC0C423, 0xFFC2C423, 0xFFC4C523, 0xFFC6C524, 0xFFC8C624, 0xFFCBC625, 0xFFCDC725,
        0xFFCFC726, 0xFFD1C826, 0xFFD3C827, 0xFFD5C927, 0xFFD7C927, 0xFFDACA28, 0xFFDCCA28, 0xFFDECB29, 0xFFE0CB29, 0xFFE2CB2A,
        0xFFE4CC2A, 0xFFE6CC2B, 0xFFE8CD2B, 0xFFEACD2B, 0xFFECCD2C, 0xFFEECD2C, 0xFFF0CD2C, 0xFFF2CD2C, 0xFFF4CD2C, 0xFFF5CD2C,
        0xFFF6CC2C, 0xFFF8CC2C, 0xFFF9CB2C, 0xFFF9CA2C, 0xFFFAC92B, 0xFFFBC82B, 0xFFFBC72A, 0xFFFCC52A, 0xFFFCC429, 0xFFFCC329,
        0xFFFCC228, 0xFFFDC028, 0xFFFDBF27, 0xFFFDBE27, 0xFFFDBC26, 0xFFFDBB26, 0xFFFDBA25, 0xFFFEB825, 0xFFFEB724, 0xFFFEB523,
        0xFFFEB423, 0xFFFEB322, 0xFFFEB122, 0xFFFEB021, 0xFFFEAF21, 0xFFFFAD20, 0xFFFFAC1F, 0xFFFFAA1F, 0xFFFFA91E, 0xFFFFA81E,
        0xFFFFA61D, 0xFFFFA51D, 0xFFFFA31C, 0xFFFFA21C, 0xFFFFA11B, 0xFFFF9F1A, 0xFFFF9E1A, 0xFFFF9C19, 0xFFFF9B19, 0xFFFF9918,
        0xFFFF9818, 0xFFFF9617, 0xFFFF9516, 0xFFFF9316, 0xFFFF9215, 0xFFFF9115, 0xFFFF8F14, 0xFFFF8E14, 0xFFFF8C13, 0xFFFF8B12,
        0xFFFF8912, 0xFFFF8811, 0xFFFF8611, 0xFFFF8410, 0xFFFF8310, 0xFFFF810F, 0xFFFF800E, 0xFFFF7E0E, 0xFFFF7D0D, 0xFFFF7B0D,
        0xFFFF790C, 0xFFFF780B, 0xFFFF760B, 0xFFFF740A, 0xFFFF730A, 0xFFFF7109, 0xFFFF6F08, 0xFFFF6E08, 0xFFFF6C07, 0xFFFF6A07,
        0xFFFF6906, 0xFFFF6706, 0xFFFF6505, 0xFFFF6305, 0xFFFF6104, 0xFFFF5F04, 0xFFFF5E03, 0xFFFF5C03, 0xFFFF5A03, 0xFFFF5802,
        0xFFFE5602, 0xFFFE5402, 0xFFFE5201, 0xFFFE4F01, 0xFFFE4D01, 0xFFFE4B00, 0xFFFE4900, 0xFFFE4600, 0xFFFE4400, 0xFFFD4100,
        0xFFFD3F00, 0xFFFD3C00, 0xFFFD3900, 0xFFFD3600, 0xFFFD3300, 0xFFFD3000,
    };
}
