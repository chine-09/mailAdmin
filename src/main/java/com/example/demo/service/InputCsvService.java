package com.example.demo.service;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.entity.VirtualDomain;
import com.example.demo.entity.VirtualUser;
import com.example.demo.repository.VirtualDomainRepository;
import com.example.demo.repository.VirtualUserRepository;
import com.opencsv.CSVReader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InputCsvService {

	private final VirtualDomainRepository domainRepository;
	private final VirtualUserRepository userRepository;
	
	public String importCsvUsers(MultipartFile file) {
        int successCount = 0;
        int errorCount = 0;

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                // 列不足や空行のスキップ
                if (line.length < 2) continue;

                String fullEmail = line[0].trim();
                String password = line[1].trim();

                int atIndex = fullEmail.lastIndexOf('@');
                if (atIndex == -1) {
                    errorCount++;
                    continue;
                }
                String username = fullEmail.substring(0, atIndex);
                String domainName = fullEmail.substring(atIndex + 1);

                try {
                    VirtualDomain domain = domainRepository.findByName(domainName);
                    if (domain == null) {
                        errorCount++;
                        continue;
                    }

                    VirtualUser user = new VirtualUser();
                    user.setVirtualDomain(domain);
                    user.setPassword(password);
                    user.setEmail(fullEmail);
                    
                    String maildir = domain.getName() + "/" + username + "/";
                    user.setMaildir(maildir);

                    userRepository.save(user);
                    successCount++;

                } catch (Exception e) {
                    errorCount++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "エラーが発生しました: " + e.getMessage();
        }

        return successCount + "件登録しました (エラー/スキップ: " + errorCount + "件)";
    }
	
}
