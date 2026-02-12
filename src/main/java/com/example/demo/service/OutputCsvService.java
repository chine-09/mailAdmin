package com.example.demo.service;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.entity.VirtualUser;
import com.example.demo.repository.VirtualUserRepository;
import com.opencsv.CSVWriter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutputCsvService {

    private final VirtualUserRepository userRepository;

    /**
     * 全ユーザのCSVデータをバイト配列として生成する
     */
    public byte[] generateUserListCsv() {
        List<VirtualUser> users = userRepository.findAll();

        try (StringWriter sw = new StringWriter();
             CSVWriter writer = new CSVWriter(sw,
                     CSVWriter.DEFAULT_SEPARATOR,
                     CSVWriter.NO_QUOTE_CHARACTER, // 余計なクォートを付けない設定
                     CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                     CSVWriter.DEFAULT_LINE_END)) {

            // ヘッダー行（必要なければ削除してください）
            // writer.writeNext(new String[]{"メールアドレス", "パスワード"});

            for (VirtualUser user : users) {
                String[] line = {
                    user.getEmail(),
                    "*****" // パスワードは伏せ字
                };
                writer.writeNext(line);
            }
            
            // CSV文字列をバイト配列に変換（BOM付きUTF-8にしてExcel文字化けを防ぐ）
            // ※今回はシンプルにUTF-8のみとします
            return sw.toString().getBytes(StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("CSV出力エラー", e);
        }
    }
}