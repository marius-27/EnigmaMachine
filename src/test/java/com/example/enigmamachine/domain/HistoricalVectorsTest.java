package com.example.enigmamachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Vectors verified against published sources:
 * Barbarossa message: http://wiki.franklinheath.co.uk/index.php/Enigma/Sample_Messages
 * and http://wiki.franklinheath.co.uk/index.php/Enigma/Sample_Decrypts
 * Key sheet procedure: https://py-enigma.readthedocs.io/en/stable/guide.html
 * Wirings, notches and stepping sequences: https://en.wikipedia.org/wiki/Enigma_rotor_details
 */
class HistoricalVectorsTest {

    private static final List<RotorDefinition> BARBAROSSA_ROTORS =
            List.of(RotorDefinition.II, RotorDefinition.IV, RotorDefinition.V);
    private static final List<Integer> BARBAROSSA_RINGS = indexes("BUL");
    private static final List<String> BARBAROSSA_PLUGBOARD =
            List.of("AV", "BS", "CG", "DL", "FU", "HZ", "IN", "KM", "OW", "RX");

    @Test
    void decryptsFirstPartOfBarbarossaMessage() {
        String ciphertext = "EDPUDNRGYSZRCXNUYTPOMRMBOFKTBZREZKMLXLVEFGUEYSIOZVEQMIKUBPMM"
                + "YLKLTTDEISMDICAGYKUACTCDOMOHWXMUUIAUBSTSLRNBZSZWNRFXWFYSSXJZ"
                + "VIJHIDISHPRKLKAYUPADTXQSPINQMATLPIFSVKDASCTACDPBOPVHJK";
        String plaintext = "AUFKLXABTEILUNGXVONXKURTINOWAXKURTINOWAXNORDWESTLXSEBEZXSEBE"
                + "ZXUAFFLIEGERSTRASZERIQTUNGXDUBROWKIXDUBROWKIXOPOTSCHKAXOPOTS"
                + "CHKAXUMXEINSAQTDREINULLXUHRANGETRETENXANGRIFFXINFXRGTX";

        assertEquals(plaintext, process(barbarossaAt("BLA"), ciphertext));
    }

    @Test
    void decryptsSecondPartOfBarbarossaMessage() {
        String ciphertext = "SFBWDNJUSEGQOBHKRTAREEZMWKPPRBXOHDROEQGBBGTQVPGVKBVVGBIMHUSZ"
                + "YDAJQIROAXSSSNREHYGGRPISEZBOVMQIEMMZCYSGQDGRERVBILEKXYQIRGIR"
                + "QNRDNVRXCYYTNJR";
        String plaintext = "DREIGEHTLANGSAMABERSIQERVORWAERTSXEINSSIEBENNULLSEQSXUHRXROE"
                + "MXEINSXINFRGTXDREIXAUFFLIEGERSTRASZEMITANFANGXEINSSEQSXKMXKM"
                + "XOSTWXKAMENECXK";

        assertEquals(plaintext, process(barbarossaAt("LSD"), ciphertext));
    }

    @Test
    void encryptsMessageKeyFromKeySheet() {
        assertEquals("KCH", process(barbarossaAt("WXC"), "BLA"));
    }

    @Test
    void encryptsMessageFromKeySheet() {
        assertEquals("NIBLFMYMLLUFWCASCSSNVHAZ", process(barbarossaAt("BLA"), "THEXRUSSIANSXAREXCOMINGX"));
    }

    private static MachineConfiguration barbarossaAt(String positions) {
        return new MachineConfiguration(
                BARBAROSSA_ROTORS, Reflector.B, BARBAROSSA_RINGS, indexes(positions), BARBAROSSA_PLUGBOARD);
    }

    private static String process(MachineConfiguration configuration, String text) {
        EnigmaMachine machine = new EnigmaMachine(configuration);
        StringBuilder output = new StringBuilder();
        for (char letter : text.toCharArray()) {
            output.append(machine.encrypt(letter));
        }
        return output.toString();
    }

    private static List<Integer> indexes(String letters) {
        return letters.chars().map(letter -> Alphabet.toIndex((char) letter)).boxed().toList();
    }
}
