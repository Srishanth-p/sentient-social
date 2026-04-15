package com.sentinel.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.Main;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


// import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Controller
public class SentinelController {

    private final ObjectMapper mapper = new ObjectMapper();

    // ── Landing page ──────────────────────────────────────────────────────────
    @GetMapping("/")
    public String index() {
        return "index";
    }

    // ── Handle ZIP upload and run pipeline ────────────────────────────────────
    @PostMapping("/analyse")
    public String analyse(@RequestParam("file") MultipartFile file,
                          Model model) throws Exception {

        // Save upload to a temp file
        Path tmp = Files.createTempFile("sentinel-upload-", "-" + file.getOriginalFilename());
        file.transferTo(tmp.toFile());

        // Run your existing pipeline
        String reportPath = Main.runPipeline(tmp.toString());

        // Read the generated JSON report
        String json = Files.readString(Path.of(reportPath));
        Map<?, ?> report = mapper.readValue(json, Map.class);

        // Pass report data to the template
        model.addAttribute("report", report);
        model.addAttribute("json", json);

        // Clean up temp upload
        Files.deleteIfExists(tmp);

        return "report";
    }

    @ControllerAdvice
    class GlobalExceptionHandler {

        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public String handleMaxUpload(RedirectAttributes attrs) {
            attrs.addFlashAttribute("error", "File too large. Please upload a ZIP under 500MB.");
            return "redirect:/";
        }
    }
}
