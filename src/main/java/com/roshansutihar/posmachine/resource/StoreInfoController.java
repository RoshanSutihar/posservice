package com.roshansutihar.posmachine.resource;

import com.roshansutihar.posmachine.entity.StoreInfo;
import com.roshansutihar.posmachine.repository.StoreInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/store")
@RequiredArgsConstructor
public class StoreInfoController {
    private final StoreInfoRepository storeInfoRepository;

    @GetMapping
    public String storeInfo(Model model) {
        StoreInfo storeInfo = storeInfoRepository.findFirstByOrderByIdAsc()
                .orElse(StoreInfo.builder().build());

        // Create a copy with masked secret for display
        StoreInfo displayStore = createDisplayCopy(storeInfo);

        model.addAttribute("storeInfo", displayStore);
        model.addAttribute("isEditMode", false);
        model.addAttribute("originalSecretKey", storeInfo.getSecretKey()); // Pass original for JS
        return "store-info";
    }

    @GetMapping("/edit")
    public String editStoreInfo(Model model) {
        StoreInfo storeInfo = storeInfoRepository.findFirstByOrderByIdAsc()
                .orElse(StoreInfo.builder().build());

        // Create display copy with empty secret field for editing
        StoreInfo displayStore = createDisplayCopy(storeInfo);
        displayStore.setSecretKey(""); // Clear for edit mode

        model.addAttribute("storeInfo", displayStore);
        model.addAttribute("isEditMode", true);
        model.addAttribute("originalSecretKey", storeInfo.getSecretKey()); // Pass original for form handling
        return "store-info";
    }

    @PostMapping
    public String saveStoreInfo(@ModelAttribute StoreInfo storeInfo,
                                @RequestParam(required = false) String originalSecretKey,
                                RedirectAttributes redirectAttributes) {
        try {
            StoreInfo existing = storeInfoRepository.findFirstByOrderByIdAsc().orElse(null);

            if (existing != null) {
                storeInfo.setId(existing.getId());
                storeInfo.setCreatedAt(existing.getCreatedAt());

                // If secret key is empty or null in the form, keep the existing one
                if (storeInfo.getSecretKey() == null || storeInfo.getSecretKey().trim().isEmpty()) {
                    storeInfo.setSecretKey(existing.getSecretKey());
                }
            }

            storeInfoRepository.save(storeInfo);
            redirectAttributes.addFlashAttribute("success", "Store information saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error saving store information: " + e.getMessage());
        }
        return "redirect:/store";
    }

    private StoreInfo createDisplayCopy(StoreInfo original) {
        StoreInfo copy = new StoreInfo();
        copy.setId(original.getId());
        copy.setStoreName(original.getStoreName());
        copy.setAddress(original.getAddress());
        copy.setPhoneNumber(original.getPhoneNumber());
        copy.setEmail(original.getEmail());
        copy.setApiUrl(original.getApiUrl());
        copy.setMerchantId(original.getMerchantId());
        copy.setTerminalId(original.getTerminalId());
        copy.setCreatedAt(original.getCreatedAt());
        copy.setUpdatedAt(original.getUpdatedAt());

        // Mask the secret key for display
        String maskedSecret = maskSecretKey(original.getSecretKey());
        copy.setSecretKey(maskedSecret);

        return copy;
    }

    private String maskSecretKey(String secretKey) {
        if (secretKey == null || secretKey.isEmpty()) {
            return "";
        }

        if (secretKey.length() <= 8) {
            return "********";
        }

        // Show first 4 and last 4 characters, mask the middle
        String firstFour = secretKey.substring(0, 4);
        String lastFour = secretKey.substring(secretKey.length() - 4);
        return firstFour + "********" + lastFour;
    }
}