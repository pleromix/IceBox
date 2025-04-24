package io.github.pleromix.icebox.util;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import org.apache.commons.lang3.SystemUtils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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

    public static Image resize(File imageFile, double width) throws IOException {
        var inputImage = fileAsMat(imageFile);
        var outputImage = new Mat();
        var scale = width / inputImage.size().width;

        Imgproc.resize(inputImage, outputImage, new Size(0, 0), scale, scale, Imgproc.INTER_AREA);

        return matToImage(outputImage);
    }

    public static Mat fileAsMat(File imageFile) throws IOException {
        return Imgcodecs.imread(imageFile.getAbsolutePath(), Imgcodecs.IMREAD_UNCHANGED);
    }

    public static Image matToImage(Mat mat) {
        var width = mat.cols();
        var height = mat.rows();
        var channels = mat.channels();
        var pixels = new byte[width * height * channels];
        var argbPixels = new int[width * height];
        var image = new WritableImage(width, height);
        var pixelWriter = image.getPixelWriter();

        if (channels < 3 || channels > 4) {
            throw new IllegalArgumentException("Mat has wrong number of channels: " + channels);
        }

        mat.get(0, 0, pixels);

        for (int i = 0, j = 0; i < pixels.length; i += channels, j++) {
            var b = pixels[i] & 0xFF;
            var g = pixels[i + 1] & 0xFF;
            var r = pixels[i + 2] & 0xFF;
            var a = (channels == 4) ? (pixels[i + 3] & 0xFF) : 0xFF;
            argbPixels[j] = (a << 24) | (r << 16) | (g << 8) | b;
        }

        pixelWriter.setPixels(0, 0, width, height, PixelFormat.getIntArgbPreInstance(), argbPixels, 0, width);

        return image;
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
