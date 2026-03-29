package com.example.demo.service;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.entity.VirtualDomain;
import com.example.demo.entity.VirtualUser;
import com.example.demo.repository.VirtualDomainRepository;
import com.opencsv.CSVReader;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InputCsvService {

    private final VirtualDomainRepository domainRepository;
    private final UserService registService;
    
    // 結果をまとめて返すためのクラス
    @Data
    public static class ImportResult {
        private String summaryMessage; // 「〇件登録しました...」
        private List<String> errorDetails = new ArrayList<>(); // エラー内容のリスト
        private boolean hasError = false;
    }
    
    public ImportResult importCsvUsers(MultipartFile file) {
        ImportResult result = new ImportResult();
        int successCount = 0;
        int errorCount = 0;
        int lineNum = 0; // 行番号カウンター

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                lineNum++;
                
                // 列不足や空行のスキップ
                if (line.length < 2) continue;

                String fullEmail = line[0].trim();
                
                if (fullEmail.startsWith("\uFEFF")) {
                    fullEmail = fullEmail.substring(1);
                }
                
                String password = line[1].trim();

                int atIndex = fullEmail.lastIndexOf('@');
                if (atIndex == -1) {
                    errorCount++;
                    result.getErrorDetails().add(lineNum + "行目: 不正なメールアドレス形式です (" + fullEmail + ")");
                    continue;
                }
                String username = fullEmail.substring(0, atIndex);
                String domainName = fullEmail.substring(atIndex + 1);

                try {
                    VirtualDomain domain = domainRepository.findByName(domainName);
                    if (domain == null) {
                        errorCount++;
                        result.getErrorDetails().add(lineNum + "行目: ドメインが見つかりません (" + domainName + ")");
                        continue;
                    }
                    
                    registService.registerUser(new VirtualUser(), domain, username, password);
                    successCount++;

                } catch (IllegalArgumentException e) {
                    // 重複エラーなど
                    errorCount++;
                    result.getErrorDetails().add(lineNum + "行目: " + e.getMessage());
                } catch (Exception e) {
                    errorCount++;
                    result.getErrorDetails().add(lineNum + "行目: 予期せぬエラー (" + fullEmail + ")");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.setSummaryMessage("CSV読み込み中に重大なエラーが発生しました: " + e.getMessage());
            result.setHasError(true);
            return result;
        }

        result.setSummaryMessage(successCount + "件登録しました (エラー/スキップ: " + errorCount + "件)");
        if (errorCount > 0) {
            result.setHasError(true);
        }
        
        return result;
    }
}