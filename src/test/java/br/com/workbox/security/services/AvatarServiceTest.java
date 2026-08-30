package br.com.workbox.security.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.workbox.exceptions.InvalidImageException;
import br.com.workbox.exceptions.ResourceNotFoundException;
import br.com.workbox.security.entities.UserApi;
import br.com.workbox.security.repositories.UserApiRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class AvatarServiceTest {

    @Mock
    private UserApiRepository userApiRepository;

    @TempDir
    java.nio.file.Path tempDir;

    private AvatarService avatarService;
    private UUID userId;
    private UserApi user;

    @BeforeEach
    void setUp() {
        avatarService = new AvatarService(userApiRepository, tempDir.toString());
        userId = UUID.randomUUID();
        user = UserApi.builder().id(userId).socialName("Alice").email("alice@example.com").password("hash").build();
    }

    private byte[] validPngBytes() throws IOException {
        final var image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        final var out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Nested
    @DisplayName("store")
    class Store {

        @Test
        @DisplayName("salva o arquivo reencodado como PNG, atualiza avatarFilename e persiste o usuário")
        void storesValidImage() throws IOException {
            when(userApiRepository.findById(userId)).thenReturn(Optional.of(user));
            final var file = new MockMultipartFile("file", "avatar.png", "image/png", validPngBytes());

            avatarService.store(userId, file);

            assertThat(user.getAvatarFilename()).isNotNull().endsWith(".png");
            assertThat(Files.exists(tempDir.resolve(user.getAvatarFilename()))).isTrue();
            verify(userApiRepository).save(user);
        }

        @Test
        @DisplayName("apaga o arquivo antigo ao substituir o avatar")
        void deletesOldFileOnReplace() throws IOException {
            when(userApiRepository.findById(userId)).thenReturn(Optional.of(user));
            final var file = new MockMultipartFile("file", "avatar.png", "image/png", validPngBytes());

            avatarService.store(userId, file);
            final var firstFilename = user.getAvatarFilename();
            avatarService.store(userId, file);

            assertThat(Files.exists(tempDir.resolve(firstFilename))).isFalse();
            assertThat(user.getAvatarFilename()).isNotEqualTo(firstFilename);
        }

        @Test
        @DisplayName("lança InvalidImageException quando o arquivo está vazio")
        void rejectsEmptyFile() {
            final var file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[0]);

            assertThatThrownBy(() -> avatarService.store(userId, file))
                    .isInstanceOf(InvalidImageException.class);
            verify(userApiRepository, never()).save(any());
        }

        @Test
        @DisplayName("lança InvalidImageException quando o content-type não é permitido")
        void rejectsDisallowedContentType() throws IOException {
            final var file = new MockMultipartFile("file", "avatar.gif", "image/gif", validPngBytes());

            assertThatThrownBy(() -> avatarService.store(userId, file))
                    .isInstanceOf(InvalidImageException.class);
            verify(userApiRepository, never()).save(any());
        }

        @Test
        @DisplayName("lança InvalidImageException quando o conteúdo não é uma imagem de verdade (content-type forjado)")
        void rejectsSpoofedContentType() {
            final var file = new MockMultipartFile("file", "malicious.png", "image/png", "não é uma imagem".getBytes());

            assertThatThrownBy(() -> avatarService.store(userId, file))
                    .isInstanceOf(InvalidImageException.class);
            verify(userApiRepository, never()).save(any());
        }

        @Test
        @DisplayName("lança ResourceNotFoundException quando o usuário não existe")
        void throwsWhenUserMissing() throws IOException {
            when(userApiRepository.findById(userId)).thenReturn(Optional.empty());
            final var file = new MockMultipartFile("file", "avatar.png", "image/png", validPngBytes());

            assertThatThrownBy(() -> avatarService.store(userId, file))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("lança InvalidImageException quando não consegue ler o stream do arquivo enviado")
        void wrapsIOExceptionOnRead() throws IOException {
            final var file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getContentType()).thenReturn("image/png");
            when(file.getInputStream()).thenThrow(new IOException("boom"));

            assertThatThrownBy(() -> avatarService.store(userId, file))
                    .isInstanceOf(InvalidImageException.class);
            verify(userApiRepository, never()).findById(any());
        }

        @Test
        @DisplayName("lança InvalidImageException quando não consegue gravar a imagem em disco")
        void wrapsIOExceptionOnWrite() throws IOException {
            when(userApiRepository.findById(userId)).thenReturn(Optional.of(user));
            Files.delete(tempDir);
            final var file = new MockMultipartFile("file", "avatar.png", "image/png", validPngBytes());

            assertThatThrownBy(() -> avatarService.store(userId, file))
                    .isInstanceOf(InvalidImageException.class);
            verify(userApiRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("remove o arquivo do disco e limpa avatarFilename")
        void deletesFileAndClearsFilename() throws IOException {
            when(userApiRepository.findById(userId)).thenReturn(Optional.of(user));
            avatarService.store(userId, new MockMultipartFile("file", "avatar.png", "image/png", validPngBytes()));
            final var filename = user.getAvatarFilename();

            avatarService.delete(userId);

            assertThat(user.getAvatarFilename()).isNull();
            assertThat(Files.exists(tempDir.resolve(filename))).isFalse();
        }

        @Test
        @DisplayName("não lança exceção quando o usuário nunca teve avatar")
        void noOpWhenNoAvatar() {
            when(userApiRepository.findById(userId)).thenReturn(Optional.of(user));

            avatarService.delete(userId);

            assertThat(user.getAvatarFilename()).isNull();
        }
    }

    @Nested
    @DisplayName("load")
    class Load {

        @Test
        @DisplayName("devolve os bytes gravados em disco com content-type image/png")
        void loadsStoredBytes() throws IOException {
            when(userApiRepository.findById(userId)).thenReturn(Optional.of(user));
            avatarService.store(userId, new MockMultipartFile("file", "avatar.png", "image/png", validPngBytes()));

            final var content = avatarService.load(userId);

            assertThat(content.contentType()).isEqualTo("image/png");
            assertThat(content.bytes()).isNotEmpty();
        }

        @Test
        @DisplayName("lança ResourceNotFoundException quando o usuário não tem avatar")
        void throwsWhenNoAvatar() {
            when(userApiRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> avatarService.load(userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("lança ResourceNotFoundException quando o usuário não existe")
        void throwsWhenUserMissing() {
            when(userApiRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> avatarService.load(userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("lança ResourceNotFoundException quando o arquivo referenciado não existe mais em disco")
        void throwsWhenFileMissingFromDisk() throws IOException {
            when(userApiRepository.findById(userId)).thenReturn(Optional.of(user));
            avatarService.store(userId, new MockMultipartFile("file", "avatar.png", "image/png", validPngBytes()));
            Files.delete(tempDir.resolve(user.getAvatarFilename()));

            assertThatThrownBy(() -> avatarService.load(userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("AvatarContent.equals/hashCode/toString")
    class AvatarContentEqualsHashCodeToString {

        @Test
        @DisplayName("iguais quando bytes e content-type têm o mesmo conteúdo")
        void equalWhenSameContent() {
            final var a = new AvatarService.AvatarContent(new byte[]{1, 2, 3}, "image/png");
            final var b = new AvatarService.AvatarContent(new byte[]{1, 2, 3}, "image/png");

            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
            assertThat(a).isEqualTo(a);
        }

        @Test
        @DisplayName("diferentes quando o conteúdo dos bytes difere")
        void notEqualWhenBytesDiffer() {
            final var a = new AvatarService.AvatarContent(new byte[]{1, 2, 3}, "image/png");
            final var b = new AvatarService.AvatarContent(new byte[]{9, 9, 9}, "image/png");

            assertThat(a).isNotEqualTo(b);
        }

        @Test
        @DisplayName("diferente de null e de outro tipo")
        void notEqualToNullOrOtherType() {
            final var a = new AvatarService.AvatarContent(new byte[]{1, 2, 3}, "image/png");

            assertThat(a).isNotEqualTo(null);
            assertThat(a).isNotEqualTo("não é um AvatarContent");
        }

        @Test
        @DisplayName("toString inclui o content-type")
        void toStringIncludesContentType() {
            final var a = new AvatarService.AvatarContent(new byte[]{1, 2, 3}, "image/png");

            assertThat(a.toString()).contains("image/png");
        }
    }
}
