package com.eema.backend.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
public final class MediaFixtures {
    private MediaFixtures() {}
    public static byte[] bytes(String name) {
        try(var stream=MediaFixtures.class.getResourceAsStream("/media/"+name)) {
            if(stream==null) throw new IllegalArgumentException("Missing QA media fixture");
            return stream.readAllBytes();
        } catch(IOException e) {throw new UncheckedIOException(e);}
    }
    // Valid decodable JPEG with trailing padding; generated at runtime, not committed as a 5MiB artifact.
    public static byte[] paddedJpeg(int length) {return Arrays.copyOf(bytes("qa.jpg"),length);}
}
