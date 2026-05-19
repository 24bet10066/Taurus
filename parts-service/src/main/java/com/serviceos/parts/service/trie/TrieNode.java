package com.serviceos.parts.service.trie;

import com.serviceos.parts.dto.response.PartSearchResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TrieNode {

    final Map<Character, TrieNode> children = new HashMap<>();

    /** Parts whose search token terminates at this node. */
    final List<PartSearchResult> results = new ArrayList<>();
}
