=== SMART E-COMMERCE PERFORMANCE REPORT ===
Generated: Fri Apr 24 00:01:51 CAT 2026

SCENARIO 1: NO INDEXES / NO CACHE
---------------------------------
Q1: Search 'laptop'            : 1.88 ms (avg of 5 runs)
Q2: Join Products/Categories   : 1.22 ms (avg of 5 runs)
Q3: Filter Category 1          : 1.12 ms (avg of 5 runs)

SCENARIO 2: INDEXES ONLY (NO CACHE)
-----------------------------------
Q1: Search 'laptop'            : 0.67 ms (avg of 5 runs)
Q2: Join Products/Categories   : 1.29 ms (avg of 5 runs)
Q3: Filter Category 1          : 1.16 ms (avg of 5 runs)

SCENARIO 3: INDEXES + CACHE
---------------------------
Q1: Search 'laptop'            : 0.79 ms (avg of 5 runs)
Q2: Join Products/Categories   : 1.43 ms (avg of 5 runs)
Q3: Filter Category 1          : 0.82 ms (avg of 5 runs)

=== PERFORMANCE ANALYSIS ===
Q1: Search 'laptop':
  Index Improvement: 64.3%
  Overall (Index+Cache) Improvement: 57.7%
Q2: Join Products/Categories:
  Index Improvement: -6.0%
  Overall (Index+Cache) Improvement: -16.9%
Q3: Filter Category 1:
  Index Improvement: -4.1%
  Overall (Index+Cache) Improvement: 26.6%

Cache Hit Rate during Scenario 3: 80% (Simulated: 4 of 5 runs)
