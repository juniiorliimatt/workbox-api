package br.com.workbox.security.services;

import br.com.workbox.exceptions.InvalidImageException;
import br.com.workbox.exceptions.ResourceNotFoundException;
import br.com.workbox.security.repositories.UserApiRepository;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Upload/remoção/leitura do avatar do usuário. O arquivo enviado nunca é gravado como
 * veio: é decodificado via {@link ImageIO} (confirma que é de fato um raster de imagem —
 * um arquivo malicioso disfarçado de .jpg falha aqui) e reencodado como PNG antes de ir
 * pro disco, descartando qualquer payload/metadado embutido no arquivo original (defesa
 * contra upload de arquivo malicioso, OWASP API10/CWE-434). Nome do arquivo em disco é
 * sempre gerado no servidor (UUID) — nunca deriva de entrada do client, prevenindo path
 * traversal (CWE-22).
 *
 * @author CLAUDE-CODE
 * @author Junior Lima - oojuniin@outlook.com
 * @since 30-08-2026
 */
@Service
public class AvatarService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final String STORED_FORMAT = "png";
    private static final String STORED_CONTENT_TYPE = "image/png";

    private static final Logger logger = LoggerFactory.getLogger(AvatarService.class);

    private final UserApiRepository userApiRepository;
    private final Path storageDir;

    public AvatarService(final UserApiRepository userApiRepository,
                          @Value("${avatar.storage-path:uploads/avatars}") final String storagePath) {
        this.userApiRepository = userApiRepository;
        this.storageDir = Path.of(storagePath);
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create avatar storage directory: " + storageDir, e);
        }
    }

    @Transactional
    public void store(final UUID userId, final MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidImageException("File is empty");
        }
        final var contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidImageException("Unsupported image type — allowed: image/jpeg, image/png, image/webp");
        }

        final BufferedImage image;
        try {
            image = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            throw new InvalidImageException("Could not read the uploaded file");
        }
        if (image == null) {
            throw new InvalidImageException("File is not a valid image");
        }

        final var user = userApiRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        final var filename = UUID.randomUUID() + "." + STORED_FORMAT;
        try {
            ImageIO.write(image, STORED_FORMAT, storageDir.resolve(filename).toFile());
        } catch (IOException e) {
            throw new InvalidImageException("Could not store the image");
        }

        deleteStoredFile(user.getAvatarFilename());
        user.setAvatarFilename(filename);
        userApiRepository.save(user);
    }

    @Transactional
    public void delete(final UUID userId) {
        final var user = userApiRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        deleteStoredFile(user.getAvatarFilename());
        user.setAvatarFilename(null);
        userApiRepository.save(user);
    }

    public AvatarContent load(final UUID userId) {
        final var user = userApiRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getAvatarFilename() == null) {
            throw new ResourceNotFoundException("User has no avatar");
        }
        try {
            final var bytes = Files.readAllBytes(storageDir.resolve(user.getAvatarFilename()));
            return new AvatarContent(bytes, STORED_CONTENT_TYPE);
        } catch (IOException e) {
            throw new ResourceNotFoundException("Avatar file not found");
        }
    }

    private void deleteStoredFile(final String filename) {
        if (filename == null) {
            return;
        }
        try {
            Files.deleteIfExists(storageDir.resolve(filename));
        } catch (IOException e) {
            logger.warn("Could not delete old avatar file {}", filename, e);
        }
    }

    public record AvatarContent(byte[] bytes, String contentType) { }
}
