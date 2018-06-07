/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.recovery.image_generator;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Command line tool to generate the font image for recovery mode.
 */
public class FontImageGenerator {
    private BufferedImage mBufferedImage;

    // We will prepare graphics for 96 characters from ' ' to 'DEL'. This is consistent with the
    // recovery code although 'DEL' is not printable.
    private String mFontChars;
    // A list of supported fonts; e.g. regular/bold version of RobotoMono.
    private List<Font> mFontList;
    private float mFontSize;

    /**
     * Exception to indicate incompatible font files used when generating the font image.
     */
    public class IllegalFontException extends Exception {
        public IllegalFontException(String s) {
            super(s);
        }
    }

    /**
     * Initializes the image with and the font file names and the fontSize.
     */
    public FontImageGenerator(String[] nameList, int fontSize) throws IOException,
        FontFormatException {

        // The font image contains characters from ' ' to 'DEL'.
        StringBuilder fontBuilder = new StringBuilder();
        for (char cur = (char) 32; cur <= (char) 127; cur++) {
            fontBuilder.append(cur);
        }
        mFontChars = fontBuilder.toString();

        // Reserve enough area for the canvas.
        mBufferedImage = new BufferedImage(fontSize * mFontChars.length() * 2,
            fontSize * nameList.length * 2,  BufferedImage.TYPE_BYTE_GRAY);

        mFontSize = fontSize;
        mFontList = loadFonts(nameList);
    }

    /**
     * @param nameList the path of the ttf files
     *
     * @return a list of font by loading the font files in nameList.
     *
     * @throws IOException if the ttf file fails to open
     * @throws FontFormatException if the ttf file doesn't have the expected format
     */
    private List<Font> loadFonts(String[] nameList) throws IOException, FontFormatException {
        List<Font> fontList = new ArrayList<>();
        for (String name : nameList) {
            try {
                File fontFile = new File(name);
                Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(mFontSize);
                fontList.add(font);
            } catch (IOException e) {
                throw new IOException("Failed to open font file " + name, e);
            }
        }

        return fontList;
    }

    /**
     * @param text the string to draw on canvas
     * @param lineSpaceAdjust the number of pixels to subtract between lines, which makes the font
     *   image more compact.
     *
     * @throws IllegalFontException if the lineWidth varies between different fonts.
     */
    private void drawText(String text, int lineSpaceAdjust) throws IllegalFontException {
        Graphics2D graphics = mBufferedImage.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);

        int y = 0;
        int lineWidth = 0;
        // Draw the text string with all the supported fonts, one font per line.
        for (Font font : mFontList) {
            graphics.setFont(font);
            FontMetrics fontMetrics = graphics.getFontMetrics();
            int lineHeight = fontMetrics.getHeight();
            int baseLine = y + lineHeight - fontMetrics.getDescent() - lineSpaceAdjust;

            // Expect all the fonts produce the same line width.
            if (lineWidth == 0) {
                lineWidth = fontMetrics.stringWidth(text);
            } else if (lineWidth != fontMetrics.stringWidth(text)) {
                throw new IllegalFontException(
                    "lineWidth is expected to be the same for all fonts");
            }

            // Draw the text at vertical offset y and increment with line space.
            graphics.drawString(text, 0, baseLine);
            y += lineHeight - lineSpaceAdjust;
        }

        resize(lineWidth, y);
    }

    /**
     * Truncates the image to |width| * |height|, starting from the top left corner.
     */
    private void resize(int width, int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphic = resizedImage.createGraphics();
        graphic.drawImage(mBufferedImage, 0, 0, null);
        graphic.dispose();

        mBufferedImage = resizedImage;
    }

    /**
     *  Saves the generated font image in png format to outputPath
     */
    private void finishAndSaveImage(String outputPath) throws IOException {
        try {
            ImageIO.write(mBufferedImage, "png", new File(outputPath));
        } catch (IOException e) {
            throw new IOException("Failed to save the generated image", e);
        }
    }

    /**
     *  Wrapper function to draw the font characters and save the result to outputPath.
     */
    public void generateImage(int lineSpaceAdjust, String outputPath) throws IllegalFontException,
        IOException {
        drawText(mFontChars, lineSpaceAdjust);
        finishAndSaveImage(outputPath);
    }

    public static void printUsage() {
        System.out.println("Usage: Java -jar $path_to_FontImageGenerator.jar $regular_font"
            + " $bold_font $output_path");
    }

    public static void main(String[] args) throws IOException, FontFormatException,
        IllegalFontException {
        if (args.length != 3) {
            printUsage();
            throw new IllegalArgumentException("We expect " + 3 + " arguments, get " + args.length);
        }

        // text size 36f and lineSpaceAdjust 6 work well on Pixels, we can turn the size into a
        // parameter later.
        int fontSize = 36;
        int lineSpaceAdjust = 6;

        // We are using mono fonts here since each character should occupy 1/96 of the final width;
        // also we expect the locations of the regular/bold font are given by args[0] and args[1].
        String[] nameList = {args[0], args[1]};

        FontImageGenerator fontImageGenerator = new FontImageGenerator(nameList, fontSize);
        fontImageGenerator.generateImage(lineSpaceAdjust, args[2]);
    }
}
