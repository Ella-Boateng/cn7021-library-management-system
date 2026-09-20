package library;

import java.io.IOException;

public interface LibraryStore {
    LibraryState load() throws IOException;
    void save(LibraryState state) throws IOException;
}
