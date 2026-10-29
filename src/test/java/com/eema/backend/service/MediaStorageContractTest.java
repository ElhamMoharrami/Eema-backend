package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MediaStorageContractTest {
    @TempDir Path root;
    static AppProperties properties(Path root) {return new AppProperties(null,
            new AppProperties.StorageProperties("local",root.toString(),"/api/upload/files/",5242880),
            new AppProperties.CorsProperties(List.of("https://app.example")),null,null,null,null,null,null);}
    @Test void fakeImageMustBeRejectedBeforeEitherStorageWrites() {
        var fake=new MockMultipartFile("file","evil.jpg","image/jpeg","this is not a jpeg".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var local=new StorageService(properties(root));var client=mock(S3Client.class);
        var s3=new S3PublicMediaStorage(client,properties(root),"qa-public","venue-media/");
        assertAll(()->assertThrows(IllegalArgumentException.class,()->local.store(fake)),
                ()->assertThrows(IllegalArgumentException.class,()->s3.store(fake)));
    }
    @Test void publicMissingFileIs404AndMalformedMultipartIs400()throws Exception {
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                new com.eema.backend.controller.UploadController(new StorageService(properties(root))))
                .setControllerAdvice(new com.eema.backend.exception.GlobalExceptionHandler()).build();
        assertAll(
            ()->assertEquals(400,mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/upload/venue-photo").contentType("text/plain").content("QA not multipart")).andReturn().getResponse().getStatus()),
            ()->assertEquals(404,mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/upload/files/11111111-1111-4111-8111-111111111111.jpg")).andReturn().getResponse().getStatus()),
            ()->assertEquals(400,mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/upload/venue-photo")).andReturn().getResponse().getStatus()),
            ()->assertEquals(400,mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/upload/venue-photo").file(new MockMultipartFile("file","evil.jpg","image/jpeg","this is not a jpeg".getBytes()))).andReturn().getResponse().getStatus()));
    }

    static byte[] image(String ext) {return com.eema.backend.support.MediaFixtures.bytes("qa."+ext);}
    static String type(String ext) {return ext.equals("jpg")||ext.equals("jpeg")?"image/jpeg":"image/"+ext;}
    long files()throws Exception {try(var paths=Files.list(root)){return paths.count();}}
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"jpg","jpeg","png","webp"})
    void realFormatsRoundTripLocalAndS3WithoutChangingBytes(String ext)throws Exception {
        byte[] bytes=image(ext);var file=new MockMultipartFile("file","../../client."+ext,type(ext),bytes);
        var local=new StorageService(properties(root));String ref=local.store(file);String name=ref.substring(ref.lastIndexOf('/')+1);
        assertTrue(ref.matches("/api/upload/files/[0-9a-f-]{36}\\."+ext));
        assertArrayEquals(bytes,local.loadAsResource(name).getContentAsByteArray());
        assertArrayEquals(bytes,new StorageService(properties(root)).loadAsResource(name).getContentAsByteArray());
        var client=mock(S3Client.class);var s3=new S3PublicMediaStorage(client,properties(root),"qa-public","media/");
        String s3ref=s3.store(file);String key="media/"+s3ref.substring(s3ref.lastIndexOf('/')+1);
        var put=org.mockito.ArgumentCaptor.forClass(software.amazon.awssdk.services.s3.model.PutObjectRequest.class);
        var body=org.mockito.ArgumentCaptor.forClass(software.amazon.awssdk.core.sync.RequestBody.class);
        verify(client).putObject(put.capture(),body.capture());assertEquals("qa-public",put.getValue().bucket());
        assertEquals(key,put.getValue().key());assertEquals(type(ext),put.getValue().contentType());assertEquals("*",put.getValue().ifNoneMatch());
        try(var stream=body.getValue().contentStreamProvider().newStream()){assertArrayEquals(bytes,stream.readAllBytes());}
        when(client.getObjectAsBytes(any(software.amazon.awssdk.services.s3.model.GetObjectRequest.class)))
                .thenReturn(software.amazon.awssdk.core.ResponseBytes.fromByteArray(software.amazon.awssdk.services.s3.model.GetObjectResponse.builder().contentType(type(ext)).build(),bytes));
        assertArrayEquals(bytes,s3.loadAsResource(key.substring(6)).getContentAsByteArray());s3.delete(s3ref);
        verify(client).deleteObject(org.mockito.ArgumentMatchers.argThat((software.amazon.awssdk.services.s3.model.DeleteObjectRequest r)->r.bucket().equals("qa-public")&&r.key().equals(key)));
        local.delete(ref);assertEquals(0,files());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"qa-alpha.webp","qa-lossless.webp"})
    void webpAlphaAndLosslessRemainSupported(String name)throws Exception {
        var bytes=com.eema.backend.support.MediaFixtures.bytes(name);
        var local=new StorageService(properties(root));
        String ref=local.store(new MockMultipartFile("file",name,"image/webp",bytes));
        assertArrayEquals(bytes,local.loadAsResource(ref.substring(ref.lastIndexOf('/')+1)).getContentAsByteArray());
    }

    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> invalid() {
        return java.util.stream.Stream.of(
            org.junit.jupiter.params.provider.Arguments.of("empty","x.jpg","image/jpeg",new byte[0]),
            org.junit.jupiter.params.provider.Arguments.of("one-byte","x.jpg","image/jpeg",new byte[]{1}),
            org.junit.jupiter.params.provider.Arguments.of("truncated","x.jpg","image/jpeg",java.util.Arrays.copyOf(image("jpg"),50)),
            org.junit.jupiter.params.provider.Arguments.of("mime-extension","x.jpg","image/png",image("png")),
            org.junit.jupiter.params.provider.Arguments.of("jpeg-png","x.png","image/jpeg",image("jpg")),
            org.junit.jupiter.params.provider.Arguments.of("webp-jpg","x.jpg","image/webp",image("webp")),
            org.junit.jupiter.params.provider.Arguments.of("actual-bytes-mismatch","x.png","image/png",image("jpg")),
            org.junit.jupiter.params.provider.Arguments.of("gif","x.gif","image/gif",new byte[]{1}),
            org.junit.jupiter.params.provider.Arguments.of("svg","x.svg","image/svg+xml","<svg/>".getBytes()),
            org.junit.jupiter.params.provider.Arguments.of("bmp","x.bmp","image/bmp",new byte[]{1}),
            org.junit.jupiter.params.provider.Arguments.of("text","x.jpg","text/plain",image("jpg")),
            org.junit.jupiter.params.provider.Arguments.of("octet","x.jpg","application/octet-stream",image("jpg")),
            org.junit.jupiter.params.provider.Arguments.of("no-mime","x.jpg",null,image("jpg")),
            org.junit.jupiter.params.provider.Arguments.of("no-extension","x","image/jpeg",image("jpg")),
            org.junit.jupiter.params.provider.Arguments.of("no-name",null,"image/jpeg",image("jpg")),
            org.junit.jupiter.params.provider.Arguments.of("over-limit","x.jpg","image/jpeg",com.eema.backend.support.MediaFixtures.paddedJpeg(5242881)),
            org.junit.jupiter.params.provider.Arguments.of("width-budget","x.png","image/png",com.eema.backend.support.MediaFixtures.bytes("too-wide.png")),
            org.junit.jupiter.params.provider.Arguments.of("pixel-budget","x.png","image/png",com.eema.backend.support.MediaFixtures.bytes("too-many-pixels.png")));
    }
    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.MethodSource("invalid")
    void invalidContentNeverCreatesAnObject(String label,String name,String type,byte[] bytes)throws Exception {
        var file=new MockMultipartFile("file",name,type,bytes);var client=mock(S3Client.class);
        assertThrows(IllegalArgumentException.class,()->new StorageService(properties(root)).store(file),label);
        assertThrows(IllegalArgumentException.class,()->new S3PublicMediaStorage(client,properties(root),"qa-public","media/").store(file),label);
        assertEquals(0,files());verifyNoInteractions(client);
    }
    @Test void exactFiveMiBIsAllowedButLyingStreamSizeIsStillBounded()throws Exception {
        byte[] bytes=com.eema.backend.support.MediaFixtures.paddedJpeg(5242880);
        var file=new MockMultipartFile("file","x.jpg","image/jpeg",bytes);var local=new StorageService(properties(root));
        String ref=local.store(file);assertEquals(5242880,local.loadAsResource(ref.substring(ref.lastIndexOf('/')+1)).contentLength());
        var client=mock(S3Client.class);new S3PublicMediaStorage(client,properties(root),"qa-public","media/").store(file);
        var lying=new MockMultipartFile("file","x.jpg","image/jpeg",com.eema.backend.support.MediaFixtures.paddedJpeg(5242881)) {
            @Override public long getSize(){return 1;}
        };
        assertThrows(IllegalArgumentException.class,()->local.store(lying));assertEquals(1,files());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"../../x.jpg","..\\..\\x.jpg","/path/x.jpg","C:\\temp\\x.jpg","%2e%2e%2fx.jpg","a∕b.jpg"})
    void clientPathsNeverChooseStoragePaths(String name)throws Exception {
        var local=new StorageService(properties(root));String ref=local.store(new MockMultipartFile("file",name,"image/jpeg",image("jpg")));
        assertTrue(ref.matches("/api/upload/files/[0-9a-f-]{36}\\.jpg"));assertEquals(1,files());
    }
    @Test void publicReadsRejectTraversalPrivateNamesSymlinksAndS3MissingKeys()throws Exception {
        var local=new StorageService(properties(root));Files.writeString(root.resolve("private.pdf"),"QA private document");
        for(String name:List.of("../private.pdf","private.pdf","../../etc/passwd","not-a-uuid.jpg","%2e%2e%2fprivate.pdf"))
            assertThrows(com.eema.backend.exception.NotFoundException.class,()->local.loadAsResource(name));
        String link="11111111-1111-4111-8111-111111111111.jpg";Files.createSymbolicLink(root.resolve(link),root.resolve("private.pdf"));
        assertThrows(com.eema.backend.exception.NotFoundException.class,()->local.loadAsResource(link));
        var client=mock(S3Client.class);var s3=new S3PublicMediaStorage(client,properties(root),"qa-public","media/");
        assertThrows(com.eema.backend.exception.NotFoundException.class,()->s3.loadAsResource("../private.pdf"));verifyNoInteractions(client);
        when(client.getObjectAsBytes(any(software.amazon.awssdk.services.s3.model.GetObjectRequest.class)))
                .thenThrow(software.amazon.awssdk.services.s3.model.NoSuchKeyException.builder().message("QA absent").build());
        assertThrows(com.eema.backend.exception.NotFoundException.class,()->s3.loadAsResource(link));
    }
    @Test void validationPreservesMetadataAndDoesNotReencode()throws Exception {
        byte[] bytes=com.eema.backend.support.MediaFixtures.bytes("qa-metadata.jpg");var local=new StorageService(properties(root));
        String ref=local.store(new MockMultipartFile("file","qa.jpg","image/jpeg",bytes));
        assertArrayEquals(bytes,local.loadAsResource(ref.substring(ref.lastIndexOf('/')+1)).getContentAsByteArray());
        assertTrue(new String(bytes,java.nio.charset.StandardCharsets.ISO_8859_1).contains("QA SYNTHETIC METADATA"));
    }

}
