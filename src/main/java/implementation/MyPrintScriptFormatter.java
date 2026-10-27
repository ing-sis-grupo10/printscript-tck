package implementation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import interpreter.PrintScriptFormatter;

import printscript.formatter.FormattingRules;
import printscript.formatter.FormattingRulesLoader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import printscript.common.Version;

public class MyPrintScriptFormatter implements PrintScriptFormatter {

    @Override
    public void format(InputStream src, String version, InputStream config, Writer writer) {
        JsonObject original = JsonParser.parseReader(new InputStreamReader(config, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject translated = translate(original);

        FormattingRules rules = new FormattingRulesLoader().load(new StringReader(translated.toString()));

        Reader srcReader = new InputStreamReader(src, StandardCharsets.UTF_8);
        new printscript.formatter.PrintScriptFormatter(rules).format(srcReader, writer, Version.from(version));
    }

    private JsonObject translate(JsonObject original) {
        JsonObject translated = new JsonObject();

        if (getBoolean(original, "enforce-no-spacing-around-equals")) {
            translated.addProperty("assignment_space_before_equals", false);
            translated.addProperty("assignment_space_after_equals", false);
        }
        if (getBoolean(original, "enforce-spacing-around-equals")) {
            translated.addProperty("assignment_space_before_equals", true);
            translated.addProperty("assignment_space_after_equals", true);
        }
        if (original.has("enforce-spacing-before-colon-in-declaration")) {
            translated.addProperty(
                    "declaration_space_before_colon",
                    original.get("enforce-spacing-before-colon-in-declaration").getAsBoolean());
        }
        if (original.has("enforce-spacing-after-colon-in-declaration")) {
            translated.addProperty(
                    "declaration_space_after_colon",
                    original.get("enforce-spacing-after-colon-in-declaration").getAsBoolean());
        }
        if (original.has("mandatory-single-space-separation")) {
            translated.addProperty(
                    "single_space_separation",
                    original.get("mandatory-single-space-separation").getAsBoolean());
        }
        if (original.has("line-breaks-after-println")) {
            translated.add("println_new_lines_after_call", original.get("line-breaks-after-println"));
        }
        if (getBoolean(original, "if-brace-below-line")) {
            translated.addProperty("if_brace_same_line", false);
        }
        if (getBoolean(original, "if-brace-same-line")) {
            translated.addProperty("if_brace_same_line", true);
        }
        if (original.has("indent-inside-if")) {
            translated.add("if_indent_size", original.get("indent-inside-if"));
        }

        return translated;
    }

    private boolean getBoolean(JsonObject json, String key) {
        return json.has(key) && json.get(key).getAsBoolean();
    }
}