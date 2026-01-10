package com.roshansutihar.posmachine.resource;

import com.roshansutihar.posmachine.entity.Product;
import com.roshansutihar.posmachine.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductRepository productRepository;

    @GetMapping
    public String listProducts(Model model) {
        model.addAttribute("products", productRepository.findAll());
        model.addAttribute("product", new Product());
        return "products";
    }

    @PostMapping
    public String addProduct(@ModelAttribute Product product, RedirectAttributes redirectAttributes) {
        try {
            productRepository.save(product);
            redirectAttributes.addFlashAttribute("success", "Product added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding product: " + e.getMessage());
        }
        return "redirect:/products";
    }
    @GetMapping("/all")
    @ResponseBody
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @GetMapping("/{id}/edit")
    public String editProduct(@PathVariable Long id, Model model) {
        productRepository.findById(id).ifPresent(product -> model.addAttribute("product", product));
        return "edit-product";
    }

    @PostMapping("/{id}")
    public String updateProduct(@PathVariable Long id, @ModelAttribute Product product, RedirectAttributes redirectAttributes) {
        try {
            product.setId(id);
            productRepository.save(product);
            redirectAttributes.addFlashAttribute("success", "Product updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating product: " + e.getMessage());
        }
        return "redirect:/products";
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        try {
            Optional<Product> product = productRepository.findById(id);
            if (product.isPresent()) {
                return ResponseEntity.ok(product.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Product not found with ID: " + id));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving product: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Product deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting product: " + e.getMessage());
        }
        return "redirect:/products";
    }


    @GetMapping("/by-upc/{upc}")
    @ResponseBody
    public ResponseEntity<?> getProductByUpc(@PathVariable String upc) {
        try {
            Optional<Product> product = productRepository.findByUpc(upc);
            if (product.isPresent()) {
                return ResponseEntity.ok(product.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Product not found with UPC: " + upc));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving product: " + e.getMessage()));
        }
    }


    @GetMapping("/search")
    public String searchProductsByUpc(@RequestParam(required = false) String upc, Model model) {
        if (upc != null && !upc.trim().isEmpty()) {
            Optional<Product> product = productRepository.findByUpc(upc.trim());
            if (product.isPresent()) {
                model.addAttribute("searchResults", List.of(product.get()));
                model.addAttribute("searchMessage", "Found product with UPC: " + upc);
            } else {
                model.addAttribute("searchResults", List.of());
                model.addAttribute("searchMessage", "No product found with UPC: " + upc);
            }
        } else {
            model.addAttribute("searchResults", List.of());
            model.addAttribute("searchMessage", "Please enter a UPC to search");
        }

        model.addAttribute("products", productRepository.findAll());
        model.addAttribute("product", new Product());
        return "products";
    }
}