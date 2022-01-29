package com.example.apples;

import java.awt.Robot;
import java.io.File;

import javax.imageio.ImageIO;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Toolkit;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

public class App {

    static final int WIDTH = 17;
    static final int HEIGHT = 10;
    static final int TOP_CORNER_X = 638;
    static final int TOP_CORNER_Y = 233;
    static final int APPLE_SIZE = 73;
    // kinda arbitrary, 765 is max cuz 255 * 3 = 765
    static final int WHITE_THRESHOLD = 650;

    static int[][] apples;
    static boolean RUN_FOREVER = true;
    static int HIGHSCORE = 151;
    static int scores = 0;

    public static void main(String[] args) throws Exception {
        if (RUN_FOREVER) {
            while (true) {
                int score = runBot();
                Robot bot = new Robot();
                bot.delay(5000);
                if (score > HIGHSCORE) {
                    BufferedImage img = bot
                            .createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
                    ImageIO.write(img, "png", new File("./highscore" + scores + ".png"));
                }
            }
        } else {
            runBot();
        }
    }

    // returns the final score
    static int runBot() {
        getApples();
        int applesCleared = calculateMove();
        int score = applesCleared;
        while (applesCleared > 0) {
            applesCleared = calculateMove();
            score += applesCleared;
        }
        return score;
    }

    static void getApples() {
        apples = new int[HEIGHT][WIDTH];
        try {
            Robot bot = new Robot();
            // pause for 0.5s, switch tabs to apple game in this time
            bot.delay(500);
            // press reset and play buttons
            int mask = InputEvent.BUTTON1_DOWN_MASK;
            bot.mouseMove(650, 1050);
            bot.mousePress(mask);
            bot.mouseRelease(mask);
            bot.delay(100);
            bot.mouseMove(1000, 650);
            bot.mousePress(mask);
            bot.mouseRelease(mask);
            bot.delay(500);
            // take screenshot, rectangle is set for the bounds of the apple box
            BufferedImage img = bot.createScreenCapture(new Rectangle(TOP_CORNER_X, TOP_CORNER_Y, 1270, 772));
            ImageIO.write(img, "png", new File("./screenshot.png"));
            // binarize
            for (int x = 0; x < img.getWidth(); x++) {
                for (int y = 0; y < img.getHeight(); y++) {
                    final int clr = img.getRGB(x, y);
                    final int red = (clr & 0x00ff0000) >> 16;
                    final int green = (clr & 0x0000ff00) >> 8;
                    final int blue = clr & 0x000000ff;
                    // above white threshold and red-ish (cuz the apple numbers are red-ish)
                    if (red + green + blue > WHITE_THRESHOLD && green <= red && green <= blue) {
                        img.setRGB(x, y, new Color(255, 255, 255).getRGB());
                    } else {
                        img.setRGB(x, y, new Color(0, 0, 0).getRGB());
                    }
                }
            }
            ImageIO.write(img, "png", new File("./binarized.png"));

            // do OCR
            final ITesseract instance = new Tesseract();
            instance.setVariable("tessedit_char_whitelist", "0123456789");
            final String result = instance.doOCR(img);

            // convert to 2d array
            int row = 0;
            int col = 0;
            for (int i = 0; i < result.length(); i++) {
                char c = result.charAt(i);
                if (c == '\n') {
                    row++;
                    col = 0;
                } else if (c != ' ') {
                    // convert char to int
                    apples[row][col] = c - '0';
                    ++col;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void performClick(int row1, int col1, int row2, int col2) {
        try {
            // bot click
            Robot bot = new Robot();
            int mask = InputEvent.BUTTON1_DOWN_MASK;
            bot.mouseMove(TOP_CORNER_X + col1 * APPLE_SIZE, TOP_CORNER_Y + row1 * APPLE_SIZE);
            bot.mousePress(mask);
            bot.mouseMove(TOP_CORNER_X + (col2 + 1) * APPLE_SIZE, TOP_CORNER_Y + (row2 + 1) * APPLE_SIZE);
            // bot.delay(400);
            bot.mouseRelease(mask);
            // clear from data
            for (int i = 0; i <= row2 - row1; i++) {
                for (int j = 0; j <= col2 - col1; j++) {
                    apples[row1 + i][col1 + j] = 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // returns the number of apples cleared
    static int calculateMove() {
        for (int row = 0; row < apples.length; row++) {
            for (int col = 0; col < apples[row].length; col++) {
                // checks every box that the mouse can make
                for (int box_height = 1; box_height < apples.length - row + 1; box_height++) {
                    for (int box_width = 1; box_width < apples[row].length - col + 1; box_width++) {
                        // sum the apples in the box
                        int count = 0;
                        int sum = 0;
                        for (int y = row; y < row + box_height; y++) {
                            for (int x = col; x < col + box_width; x++) {
                                sum += apples[y][x];
                                if (apples[y][x] > 0) {
                                    count++;
                                }
                                if (sum > 10) {
                                    break;
                                }
                            }
                            if (sum > 10) {
                                break;
                            }
                        }
                        if (sum == 10) {
                            performClick(row, col, row + box_height - 1, col + box_width - 1);
                            return count;
                        } else if (sum > 10) {
                            break;
                        }
                    }
                }
            }
        }
        return 0;
    }
}
