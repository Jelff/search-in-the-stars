package labirinto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class LeitorLabirinto {
    private LeitorLabirinto() {}

    public static Labirinto ler(Path arquivo) throws IOException {
        // readAllLines aceita LF, CRLF e última linha sem quebra. Não remove linhas vazias.
        List<String> linhas = new ArrayList<>(Files.readAllLines(arquivo, StandardCharsets.UTF_8));
        if (!linhas.isEmpty() && linhas.get(0).startsWith("\uFEFF"))
            linhas.set(0, linhas.get(0).substring(1));
        return new Labirinto(linhas);
    }
}
