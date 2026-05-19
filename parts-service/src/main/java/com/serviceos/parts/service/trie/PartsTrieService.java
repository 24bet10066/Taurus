package com.serviceos.parts.service.trie;

import com.serviceos.parts.dto.response.PartSearchResult;
import com.serviceos.parts.entity.SparePart;
import com.serviceos.parts.repository.SparePartRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * In-memory prefix trie for instant parts search.
 * Tokens are individual words from name, SKU, and brand (all lowercase).
 * Fallback to PostgreSQL full-text search if trie returns no results.
 */
@Service
public class PartsTrieService {

    private static final Logger log = LoggerFactory.getLogger(PartsTrieService.class);
    private static final int MIN_PREFIX_LENGTH = 2;

    private final SparePartRepository sparePartRepository;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private TrieNode root = new TrieNode();

    public PartsTrieService(SparePartRepository sparePartRepository) {
        this.sparePartRepository = sparePartRepository;
    }

    @PostConstruct
    public void build() {
        log.info("Building parts search trie...");
        List<SparePart> parts = sparePartRepository.findAllActive();
        TrieNode newRoot = new TrieNode();
        for (SparePart p : parts) {
            insertInto(newRoot, p);
        }
        lock.writeLock().lock();
        try {
            root = newRoot;
        } finally {
            lock.writeLock().unlock();
        }
        log.info("Trie built with {} active parts", parts.size());
    }

    /** Call after creating a new part so it appears in search immediately. */
    public void insert(SparePart part) {
        lock.writeLock().lock();
        try {
            insertInto(root, part);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Full rebuild — call after bulk import. */
    public void rebuild() {
        build();
    }

    /**
     * Returns up to {@code limit} matching parts sorted by stock descending.
     * Returns empty list when prefix is shorter than MIN_PREFIX_LENGTH.
     */
    public List<PartSearchResult> search(String prefix, int limit) {
        if (prefix == null || prefix.length() < MIN_PREFIX_LENGTH) return List.of();
        String lower = prefix.trim().toLowerCase();

        lock.readLock().lock();
        try {
            TrieNode node = navigate(root, lower);
            if (node == null) return List.of();

            List<PartSearchResult> collected = new ArrayList<>();
            dfs(node, collected);
            collected.sort(Comparator.comparingInt(PartSearchResult::currentStock).reversed());
            return collected.size() <= limit ? collected : collected.subList(0, limit);
        } finally {
            lock.readLock().unlock();
        }
    }

    // -------------------------------------------------------------------------

    private void insertInto(TrieNode root, SparePart part) {
        PartSearchResult result = toSearchResult(part);
        for (String token : extractTokens(part)) {
            insertWord(root, token, result);
        }
    }

    private void insertWord(TrieNode root, String word, PartSearchResult result) {
        TrieNode current = root;
        for (char c : word.toCharArray()) {
            current = current.children.computeIfAbsent(c, k -> new TrieNode());
        }
        current.results.add(result);
    }

    private TrieNode navigate(TrieNode root, String prefix) {
        TrieNode current = root;
        for (char c : prefix.toCharArray()) {
            current = current.children.get(c);
            if (current == null) return null;
        }
        return current;
    }

    private void dfs(TrieNode node, List<PartSearchResult> acc) {
        acc.addAll(node.results);
        for (TrieNode child : node.children.values()) {
            dfs(child, acc);
        }
    }

    private Set<String> extractTokens(SparePart part) {
        Set<String> tokens = new LinkedHashSet<>();
        if (part.getName() != null) {
            splitWords(part.getName(), tokens);
        }
        if (part.getSku() != null && !part.getSku().isBlank()) {
            tokens.add(part.getSku().toLowerCase());
        }
        if (part.getBrand() != null) {
            splitWords(part.getBrand(), tokens);
        }
        return tokens;
    }

    private void splitWords(String text, Set<String> out) {
        for (String word : text.toLowerCase().split("[\\s\\-_/]+")) {
            if (word.length() >= MIN_PREFIX_LENGTH) out.add(word);
        }
    }

    private PartSearchResult toSearchResult(SparePart p) {
        return new PartSearchResult(
                p.getId(), p.getName(), p.getSku(), p.getBrand(),
                p.getCurrentStock(), p.getSellPrice(), p.getLocation()
        );
    }
}
