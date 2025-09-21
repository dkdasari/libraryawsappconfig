package com.example.library.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/library")
public class LibraryController {

    // In-memory store (just for demo)
    private final Map<Long, String> books = new HashMap<>();
    private long idCounter = 1;

    // Create a book
    @PostMapping
    public ResponseEntity<String> addBook(@RequestBody String title) {
        long id = idCounter++;
        books.put(id, title);
        return ResponseEntity.ok("Book added with ID: " + id);
    }

    // Read all books
    @GetMapping
    public ResponseEntity<Map<Long, String>> getAllBooks() {
        log.debug("fetching book information in Debug.DEBUG");
        log.info("fetching book information in Debug.INFO");
        log.warn("fetching book information in Debug.warn");
        return ResponseEntity.ok(books);
    }

    // Read single book
    @GetMapping("/{id}")
    public ResponseEntity<String> getBook(@PathVariable Long id) {
        String title = books.get(id);
        if (title == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(title);
    }

    // Update book
    @PutMapping("/{id}")
    public ResponseEntity<String> updateBook(@PathVariable Long id, @RequestBody String newTitle) {
        if (!books.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        books.put(id, newTitle);
        return ResponseEntity.ok("Book updated with ID: " + id);
    }

    // Delete book
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBook(@PathVariable Long id) {
        if (!books.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        books.remove(id);
        return ResponseEntity.ok("Book deleted with ID: " + id);
    }
}
