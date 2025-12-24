package com.nested.server.service;

import com.nested.server.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 100 * 1024; // 100KB
    private static final int MAX_AVATAR_DIMENSION = 256;
    private static final int MAX_POST_IMAGE_DIMENSION = 1920;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final Path uploadDir;
    private final Path avatarDir;
    private final Path postImageDir;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadPath) {
        this.uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        this.avatarDir = this.uploadDir.resolve("avatars");
        this.postImageDir = this.uploadDir.resolve("posts");

        try {
            Files.createDirectories(avatarDir);
            Files.createDirectories(postImageDir);
            log.info("Upload directories created at: {}", uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directories", e);
        }
    }

    public String storeAvatar(MultipartFile file, String userId) {
        validateFile(file);

        String extension = getExtension(file.getOriginalFilename());
        String filename = userId + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;

        try {
            // Process and resize avatar
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                throw new BadRequestException("Could not read image file");
            }

            BufferedImage resizedImage = resizeImage(originalImage, MAX_AVATAR_DIMENSION);
            byte[] compressedBytes = compressImage(resizedImage, extension);

            // Check final size
            if (compressedBytes.length > MAX_FILE_SIZE) {
                // Try with more compression
                compressedBytes = compressImageWithQuality(resizedImage, extension, 0.6f);
                if (compressedBytes.length > MAX_FILE_SIZE) {
                    throw new BadRequestException("Image too large. Please use a smaller image.");
                }
            }

            Path targetPath = avatarDir.resolve(filename);
            Files.copy(new ByteArrayInputStream(compressedBytes), targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Stored avatar: {} ({} bytes)", filename, compressedBytes.length);
            return "/uploads/avatars/" + filename;

        } catch (IOException e) {
            log.error("Failed to store avatar", e);
            throw new BadRequestException("Failed to store image: " + e.getMessage());
        }
    }

    public String storePostImage(MultipartFile file, String postId) {
        validateFile(file);

        String extension = getExtension(file.getOriginalFilename());
        String filename = postId + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;

        try {
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                throw new BadRequestException("Could not read image file");
            }

            BufferedImage resizedImage = resizeImage(originalImage, MAX_POST_IMAGE_DIMENSION);
            byte[] compressedBytes = compressImage(resizedImage, extension);

            // Check final size
            if (compressedBytes.length > MAX_FILE_SIZE) {
                compressedBytes = compressImageWithQuality(resizedImage, extension, 0.5f);
                if (compressedBytes.length > MAX_FILE_SIZE) {
                    throw new BadRequestException("Image too large after compression. Please use a smaller image.");
                }
            }

            Path targetPath = postImageDir.resolve(filename);
            Files.copy(new ByteArrayInputStream(compressedBytes), targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Stored post image: {} ({} bytes)", filename, compressedBytes.length);
            return "/uploads/posts/" + filename;

        } catch (IOException e) {
            log.error("Failed to store post image", e);
            throw new BadRequestException("Failed to store image: " + e.getMessage());
        }
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            Path filePath;
            if (fileUrl.startsWith("/uploads/avatars/")) {
                filePath = avatarDir.resolve(fileUrl.substring("/uploads/avatars/".length()));
            } else if (fileUrl.startsWith("/uploads/posts/")) {
                filePath = postImageDir.resolve(fileUrl.substring("/uploads/posts/".length()));
            } else {
                return; // Not a local file
            }

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted file: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", fileUrl, e);
        }
    }

    public Path getFilePath(String relativePath) {
        return uploadDir.resolve(relativePath).normalize();
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Invalid file type. Allowed: JPEG, PNG, GIF, WebP");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BadRequestException("Invalid file extension. Allowed: jpg, jpeg, png, gif, webp");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int maxDimension) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        if (width <= maxDimension && height <= maxDimension) {
            return originalImage;
        }

        double ratio = Math.min((double) maxDimension / width, (double) maxDimension / height);
        int newWidth = (int) (width * ratio);
        int newHeight = (int) (height * ratio);

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return resizedImage;
    }

    private byte[] compressImage(BufferedImage image, String extension) throws IOException {
        return compressImageWithQuality(image, extension, 0.8f);
    }

    private byte[] compressImageWithQuality(BufferedImage image, String extension, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String formatName = extension.equals("jpg") ? "jpeg" : extension;

        if (formatName.equals("jpeg")) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (writers.hasNext()) {
                ImageWriter writer = writers.next();
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);

                ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
                writer.dispose();
                ios.close();
            }
        } else {
            ImageIO.write(image, formatName, baos);
        }

        return baos.toByteArray();
    }
}
