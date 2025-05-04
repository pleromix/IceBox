package io.github.pleromix.icebox.util;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import org.apache.commons.lang3.SystemUtils;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.JarFile;

public final class Utility {

    public static final String ARCH_X86_32 = "x86_32";
    public static final String ARCH_X86_64 = "x86_64";
    public static final String ARCH_PPC = "ppc";

    public static final Map<String, String> ARCH = new HashMap<>() {
        {
            put("x86", ARCH_X86_32);
            put("i386", ARCH_X86_32);
            put("i486", ARCH_X86_32);
            put("i586", ARCH_X86_32);
            put("i686", ARCH_X86_32);
            put("x86_64", ARCH_X86_64);
            put("amd64", ARCH_X86_64);
            put("powerpc", ARCH_PPC);
        }
    };

    public static boolean isArchX86() {
        return ARCH.get(SystemUtils.OS_ARCH).equals(ARCH_X86_32);
    }

    public static boolean isArchX64() {
        return ARCH.get(SystemUtils.OS_ARCH).equals(ARCH_X86_64);
    }

    public static boolean isArchPPC() {
        return ARCH.get(SystemUtils.OS_ARCH).equals(ARCH_PPC);
    }

    public static Image resize(File imageFile, double size) throws IOException {
        var inputImage = fileAsMat(imageFile);
        var outputImage = new Mat();
        var scale = size / inputImage.size().width;

        if (inputImage.size().width > size) {
            Imgproc.resize(inputImage, outputImage, new Size(0, 0), scale, scale, Imgproc.INTER_AREA);
            return matToImage(outputImage);
        }

        return matToImage(inputImage);
    }

    public static MatOfByte webpToJpg(File imageFile) throws IOException {
        var inputImage = fileAsMat(imageFile);
        var jpgBytes = new MatOfByte();

        Imgcodecs.imencode(".jpg", inputImage, jpgBytes);

        return jpgBytes;
    }

    public static Mat fileAsMat(File imageFile) throws IOException {
        return Imgcodecs.imread(imageFile.getAbsolutePath(), Imgcodecs.IMREAD_UNCHANGED);
    }

    public static Image matToImage(Mat mat) {
        var width = mat.cols();
        var height = mat.rows();
        var channels = mat.channels();
        var depth = mat.depth();
        var converted = new Mat();
        var pixels = new byte[width * height * 4];
        var argbPixels = new int[width * height];
        var image = new WritableImage(width, height);
        var pixelWriter = image.getPixelWriter();

        switch (depth) {
            case CvType.CV_8U -> mat.copyTo(converted);
            case CvType.CV_16U, CvType.CV_32F -> {
                Core.normalize(mat, converted, 0, 255, Core.NORM_MINMAX);
                converted.convertTo(converted, CvType.CV_8U);
            }
            default -> throw new IllegalArgumentException("Unsupported Mat depth: " + depth);
        }

        switch (channels) {
            case 1 -> Imgproc.cvtColor(converted, converted, Imgproc.COLOR_GRAY2BGRA);
            case 3 -> Imgproc.cvtColor(converted, converted, Imgproc.COLOR_BGR2BGRA);
            case 4 -> {
            }
            default -> throw new IllegalArgumentException("Unsupported number of channels: " + channels);
        }

        converted.get(0, 0, pixels);

        for (int i = 0, j = 0; i < pixels.length; i += 4, j++) {
            var b = pixels[i] & 0xFF;
            var g = pixels[i + 1] & 0xFF;
            var r = pixels[i + 2] & 0xFF;
            var a = pixels[i + 3] & 0xFF;
            argbPixels[j] = (a << 24) | (r << 16) | (g << 8) | b;
        }

        pixelWriter.setPixels(0, 0, width, height, PixelFormat.getIntArgbPreInstance(), argbPixels, 0, width);

        return image;
    }

    public static Mat removeAlphaChannel(Mat mat) {
        final var channels = new ArrayList<Mat>();
        final var image = new Mat();
        final var notAlpha = new Mat();

        Core.split(mat, channels);

        var b = channels.get(0);
        var g = channels.get(1);
        var r = channels.get(2);
        var a = channels.get(3);

        Core.merge(List.of(b, g, r), image);

        Core.bitwise_not(a, notAlpha);
        Imgproc.cvtColor(notAlpha, notAlpha, Imgproc.COLOR_GRAY2BGR);
        Core.bitwise_and(image, image, image, a);
        Core.add(image, notAlpha, image);

        return image;
    }

    public static byte[] transformation(File file, double scale, int quality, boolean rotate90DegCcw, boolean rotate90DegCw, boolean flipHorizontal, boolean flipVertical, boolean isGrayscale) throws IOException {
        final var transformers = new ArrayList<Function<Mat, Mat>>();
        final var outputImage = new MatOfByte();
        final var params = new MatOfInt();

        var image = fileAsMat(file);

        if (image.channels() == 4) {
            image = removeAlphaChannel(image);
        }

        if (scale < 1.0D) {
            transformers.add(mat -> resize(mat, scale));
        }

        if (quality < 100) {
            params.fromArray(
                    Imgcodecs.IMWRITE_JPEG_QUALITY,
                    Math.min(quality, 95),
                    Imgcodecs.IMWRITE_JPEG_OPTIMIZE,
                    1
            );
        }

        if (rotate90DegCcw) {
            transformers.add(Utility::rotate90DegCcw);
        }

        if (rotate90DegCw) {
            transformers.add(Utility::rotate90DegCw);
        }

        if (flipHorizontal) {
            transformers.add(Utility::flipHorizontal);
        }

        if (flipVertical) {
            transformers.add(Utility::flipVertical);
        }

        if (isGrayscale) {
            transformers.add(Utility::convertToGrayscale);
        }

        final var transformer = transformers.stream().reduce(Function.identity(), Function::andThen);

        Imgcodecs.imencode(".jpg", transformer.apply(image), outputImage, params);

        return outputImage.toArray();
    }

    public static Mat resize(Mat input, double scale) {
        final var resized = new Mat();
        Imgproc.resize(input, resized, new Size(0.0D, 0.0D), scale, scale, Imgproc.INTER_AREA);
        return resized;
    }

    public static Mat rotate90DegCcw(Mat input) {
        final var rotated = new Mat();
        Core.rotate(input, rotated, Core.ROTATE_90_COUNTERCLOCKWISE);
        return rotated;
    }

    public static Mat rotate90DegCw(Mat input) {
        final var rotated = new Mat();
        Core.rotate(input, rotated, Core.ROTATE_90_CLOCKWISE);
        return rotated;
    }

    public static Mat flipHorizontal(Mat input) {
        final var flipped = new Mat();
        Core.flip(input, flipped, 1);
        return flipped;
    }

    public static Mat flipVertical(Mat input) {
        final var flipped = new Mat();
        Core.flip(input, flipped, 0);
        return flipped;
    }

    public static Mat convertToGrayscale(Mat input) {
        final var grayscale = new Mat();
        Imgproc.cvtColor(input, grayscale, Imgproc.COLOR_RGB2GRAY);
        return grayscale;
    }

    public static double normalize(double value, double min, double max) {
        if (max == min) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        return (value - min) / (max - min);
    }

    public static String getAppVersion() {
        try {
            var resource = Utility.class.getProtectionDomain().getCodeSource().getLocation();

            try (var jarFile = new JarFile(new File(resource.toURI()))) {
                return jarFile.getManifest().getMainAttributes().getValue("Implementation-Version");
            }
        } catch (Exception e) {
            return null;
        }
    }
}
