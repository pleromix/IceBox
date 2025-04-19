package io.github.pleromix.icebox.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

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

    public static File resize(File imageFile, double width) throws IOException {
        var path = System.getProperty("java.io.tmpdir");
        var tempImageFile = Paths.get(path, String.format("%s.jpg", UUID.randomUUID())).toFile();
        var inputImage = getFileAsImage(imageFile);
        var outputImage = new Mat();
        var scale = width / inputImage.size().width;

        Imgproc.resize(inputImage, outputImage, new Size(0, 0), scale, scale, Imgproc.INTER_AREA);
        Imgcodecs.imwrite(tempImageFile.getPath(), outputImage);

        return tempImageFile;
    }

    public static Mat getFileAsImage(File imageFile) throws IOException {
        var byteArray = FileUtils.readFileToByteArray(imageFile);
        return Imgcodecs.imdecode(new MatOfByte(byteArray), Imgcodecs.IMREAD_UNCHANGED);
    }

    public static String getAppVersion() {
        /*try (var manifestStream = Utility.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF")) {
            var manifest = new Manifest(manifestStream);
            return manifest.getMainAttributes().getValue("Implementation-Version");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;*/

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
