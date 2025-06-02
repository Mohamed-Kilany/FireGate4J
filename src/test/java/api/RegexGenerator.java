package api;

import com.github.curiousoddman.rgxgen.RgxGen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RegexGenerator {
    private static final Logger logger = LogManager.getLogger(RegexGenerator.class);

    public Object generate(String regex, String type) {
        RgxGen rgxGen = RgxGen.parse(regex);
        String result = rgxGen.generate();

        try {
            logger.debug("""
                    
                    ╔═══════════════════════════════════════════════════
                    ║ GENERATED VALUE
                    ╠═ REGEX: {}
                    ╠═ TYPE: {}
                    ╠═ RESULT: {}
                    ╚═══════════════════════════════════════════════════""", regex, type, result);
        } catch (Exception e) {
            logger.error("Failed to log request", e);
        }
        return new TypeConverter().convert(result, type);
    }
}
