package com.roshansutihar.posmachine.resource;

import com.roshansutihar.posmachine.entity.StoreInfo;
import com.roshansutihar.posmachine.repository.StoreInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
        model.addAttribute("storeInfo", storeInfo);
        return "store-info";
    }

    @PostMapping
    public String saveStoreInfo(@ModelAttribute StoreInfo storeInfo, RedirectAttributes redirectAttributes) {
        try {

            StoreInfo existing = storeInfoRepository.findFirstByOrderByIdAsc().orElse(null);
            if (existing != null) {
                storeInfo.setId(existing.getId());
                storeInfo.setCreatedAt(existing.getCreatedAt());
            }

            storeInfoRepository.save(storeInfo);
            redirectAttributes.addFlashAttribute("success", "Store information saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error saving store information: " + e.getMessage());
        }
        return "redirect:/store";
    }
}
