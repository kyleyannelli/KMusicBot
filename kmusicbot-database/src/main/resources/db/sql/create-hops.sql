DROP TABLE IF EXISTS `v_id_relationships`;
DROP TABLE IF EXISTS `v_id_paths`;
DROP PROCEDURE IF EXISTS `ComputeAllPaths`;
DROP PROCEDURE IF EXISTS `ManualComputeAllPaths`;
DROP PROCEDURE IF EXISTS `RecomputePathsAfterDelete`;
DROP PROCEDURE IF EXISTS `GetRelationshipStrength`;
DROP PROCEDURE IF EXISTS `GetTopRelatedIds`;
DROP PROCEDURE IF EXISTS `GetRelationshipSummary`;
DROP PROCEDURE IF EXISTS `GetTopRelatedIdsDefault`;
DROP PROCEDURE IF EXISTS `GetTopRelatedIdsWithLimit`;
DROP PROCEDURE IF EXISTS `GetRelationshipStrengthBidirectional`;
DROP PROCEDURE IF EXISTS `GetTopXRelationships`;
DROP TRIGGER IF EXISTS `update_paths_on_insert`;
DROP TRIGGER IF EXISTS `comprehensive_cascade_delete`;

CREATE TABLE `v_id_relationships` (
    `v_id` VARCHAR(255) NOT NULL,
    `related_v_id` VARCHAR(255) NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`v_id`, `related_v_id`),
    INDEX idx_v_id (`v_id`),
    INDEX idx_related_v_id (`related_v_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `v_id_paths` (
    `start_v_id` VARCHAR(255) NOT NULL,
    `end_v_id` VARCHAR(255) NOT NULL,
    `hop_count` INT NOT NULL,
    `path` TEXT,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`start_v_id`, `end_v_id`),
    INDEX idx_start_hop (`start_v_id`, `hop_count`),
    INDEX idx_end_hop (`end_v_id`, `hop_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELIMITER $$

CREATE PROCEDURE ComputeAllPaths(IN max_hops INT)
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE current_hop INT DEFAULT 1;
    DECLARE rows_added INT DEFAULT 0;

    DELETE FROM v_id_paths;

    INSERT INTO v_id_paths (start_v_id, end_v_id, hop_count, path)
    SELECT v_id, related_v_id, 1, CONCAT(v_id, '->', related_v_id)
    FROM v_id_relationships;

    hop_loop: WHILE current_hop < max_hops DO
        SET current_hop = current_hop + 1;

        INSERT IGNORE INTO v_id_paths (start_v_id, end_v_id, hop_count, path)
        SELECT
            p.start_v_id,
            r.related_v_id,
            current_hop,
            CONCAT(p.path, '->', r.related_v_id)
        FROM v_id_paths p
        JOIN v_id_relationships r ON p.end_v_id = r.v_id
        WHERE p.hop_count = current_hop - 1
        AND NOT EXISTS (
            SELECT 1 FROM v_id_paths existing
            WHERE existing.start_v_id = p.start_v_id
            AND existing.end_v_id = r.related_v_id
        );

        SET rows_added = ROW_COUNT();

        IF rows_added = 0 THEN
            LEAVE hop_loop;
        END IF;
    END WHILE;
END$$

CREATE PROCEDURE ManualComputeAllPaths(IN max_hops INT)
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE current_hop INT DEFAULT 1;
    DECLARE rows_added INT DEFAULT 0;

    TRUNCATE TABLE v_id_paths;

    INSERT INTO v_id_paths (start_v_id, end_v_id, hop_count, path)
    SELECT v_id, related_v_id, 1, CONCAT(v_id, '->', related_v_id)
    FROM v_id_relationships;

    hop_loop: WHILE current_hop < max_hops DO
        SET current_hop = current_hop + 1;

        INSERT IGNORE INTO v_id_paths (start_v_id, end_v_id, hop_count, path)
        SELECT
            p.start_v_id,
            r.related_v_id,
            current_hop,
            CONCAT(p.path, '->', r.related_v_id)
        FROM v_id_paths p
        JOIN v_id_relationships r ON p.end_v_id = r.v_id
        WHERE p.hop_count = current_hop - 1
        AND NOT EXISTS (
            SELECT 1 FROM v_id_paths existing
            WHERE existing.start_v_id = p.start_v_id
            AND existing.end_v_id = r.related_v_id
        );

        SET rows_added = ROW_COUNT();

        IF rows_added = 0 THEN
            LEAVE hop_loop;
        END IF;
    END WHILE;
END$$

CREATE PROCEDURE RecomputePathsAfterDelete(IN deleted_v_id VARCHAR(255), IN deleted_related_v_id VARCHAR(255))
BEGIN
    DELETE FROM v_id_paths
    WHERE path LIKE CONCAT('%', deleted_v_id, '->', deleted_related_v_id, '%');

    CALL ComputeAllPaths(100);
END$$

CREATE PROCEDURE GetRelationshipStrength(
    IN source_v_id VARCHAR(255),
    IN target_v_id VARCHAR(255),
    OUT is_related BOOLEAN,
    OUT shortest_hops INT,
    OUT total_paths INT,
    OUT shortest_path TEXT
)
BEGIN
    DECLARE path_count INT DEFAULT 0;
    DECLARE min_hops INT DEFAULT NULL;
    DECLARE sample_path TEXT DEFAULT NULL;

    SELECT COUNT(*), MIN(hop_count), MIN(path)
    INTO path_count, min_hops, sample_path
    FROM v_id_paths
    WHERE start_v_id = source_v_id AND end_v_id = target_v_id;

    SET is_related = (path_count > 0);
    SET shortest_hops = min_hops;
    SET total_paths = path_count;
    SET shortest_path = sample_path;
END$$

CREATE PROCEDURE GetTopRelatedIds(
    IN source_v_id VARCHAR(255),
    IN limit_count INT,
    IN max_hops INT
)
BEGIN
    DECLARE hop_filter_clause TEXT DEFAULT '';
    DECLARE actual_limit INT DEFAULT 100;

    IF limit_count IS NULL THEN
        SET actual_limit = 10;
    ELSE
        SET actual_limit = limit_count;
    END IF;

    IF max_hops IS NOT NULL THEN
        SET hop_filter_clause = CONCAT(' AND hop_count <= ', max_hops);
    END IF;

    DROP TEMPORARY TABLE IF EXISTS temp_related_results;
    CREATE TEMPORARY TABLE temp_related_results (
        related_v_id VARCHAR(255),
        shortest_hops INT,
        total_paths INT,
        relationship_strength DECIMAL(10,4),
        shortest_path TEXT,
        INDEX idx_strength (relationship_strength DESC)
    );

    SET @sql = CONCAT('
        INSERT INTO temp_related_results
        SELECT
            end_v_id as related_v_id,
            MIN(hop_count) as shortest_hops,
            COUNT(*) as total_paths,
            (COUNT(*) * 1.0) / (MIN(hop_count) * MIN(hop_count)) as relationship_strength,
            (SELECT path FROM v_id_paths p2
             WHERE p2.start_v_id = ''', source_v_id, '''
             AND p2.end_v_id = p1.end_v_id
             ORDER BY hop_count LIMIT 1) as shortest_path
        FROM v_id_paths p1
        WHERE start_v_id = ''', source_v_id, '''',
        hop_filter_clause, '
        GROUP BY end_v_id
        ORDER BY relationship_strength DESC
        LIMIT ', actual_limit
    );

    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;

    SELECT * FROM temp_related_results;

    DROP TEMPORARY TABLE temp_related_results;
END$$

CREATE PROCEDURE GetRelationshipSummary(IN source_v_id VARCHAR(255))
BEGIN
    SELECT
        'Outgoing Relationships' as direction,
        hop_count,
        COUNT(*) as path_count,
        COUNT(DISTINCT end_v_id) as unique_targets
    FROM v_id_paths
    WHERE start_v_id = source_v_id
    GROUP BY hop_count

    UNION ALL

    SELECT
        'Incoming Relationships' as direction,
        hop_count,
        COUNT(*) as path_count,
        COUNT(DISTINCT start_v_id) as unique_sources
    FROM v_id_paths
    WHERE end_v_id = source_v_id
    GROUP BY hop_count

    ORDER BY direction, hop_count;
END$$

CREATE PROCEDURE GetTopRelatedIdsDefault(IN source_v_id VARCHAR(255))
BEGIN
    CALL GetTopRelatedIds(source_v_id, 100, NULL);
END$$

CREATE PROCEDURE GetTopRelatedIdsWithLimit(IN source_v_id VARCHAR(255), IN limit_count INT)
BEGIN
    CALL GetTopRelatedIds(source_v_id, limit_count, NULL);
END$$

DELIMITER ;

DELIMITER $$

CREATE TRIGGER update_paths_on_insert
AFTER INSERT ON v_id_relationships
FOR EACH ROW
BEGIN
    DECLARE max_hops INT DEFAULT 100;
    DECLARE current_hop INT DEFAULT 1;
    DECLARE new_paths_added INT DEFAULT 1;

    INSERT IGNORE INTO v_id_paths (start_v_id, end_v_id, hop_count, path)
    VALUES (NEW.v_id, NEW.related_v_id, 1, CONCAT(NEW.v_id, '->', NEW.related_v_id));

    WHILE current_hop < max_hops AND new_paths_added > 0 DO
        SET current_hop = current_hop + 1;
        SET new_paths_added = 0;

        INSERT IGNORE INTO v_id_paths (start_v_id, end_v_id, hop_count, path)
        SELECT
            p.start_v_id,
            NEW.related_v_id,
            p.hop_count + 1,
            CONCAT(p.path, '->', NEW.related_v_id)
        FROM v_id_paths p
        WHERE p.end_v_id = NEW.v_id
        AND p.hop_count = current_hop - 1
        AND p.start_v_id != NEW.related_v_id
        AND NOT EXISTS (
            SELECT 1 FROM v_id_paths existing
            WHERE existing.start_v_id = p.start_v_id
            AND existing.end_v_id = NEW.related_v_id
        );

        SET new_paths_added = ROW_COUNT();

        INSERT IGNORE INTO v_id_paths (start_v_id, end_v_id, hop_count, path)
        SELECT
            NEW.v_id,
            p.end_v_id,
            p.hop_count + 1,
            CONCAT(NEW.v_id, '->', p.path)
        FROM v_id_paths p
        WHERE p.start_v_id = NEW.related_v_id
        AND p.hop_count = current_hop - 1
        AND p.end_v_id != NEW.v_id
        AND NOT EXISTS (
            SELECT 1 FROM v_id_paths existing
            WHERE existing.start_v_id = NEW.v_id
            AND existing.end_v_id = p.end_v_id
        );

        SET new_paths_added = new_paths_added + ROW_COUNT();
    END WHILE;

END$$

CREATE TRIGGER comprehensive_cascade_delete
AFTER DELETE ON v_id_relationships
FOR EACH ROW
BEGIN
    CALL RecomputePathsAfterDelete(OLD.v_id, OLD.related_v_id);
END$$

DELIMITER ;

DELIMITER $$

CREATE PROCEDURE GetRelationshipStrengthBidirectional(
    IN id1 VARCHAR(255),
    IN id2 VARCHAR(255)
)
BEGIN
    SELECT
        id1 as id_1,
        id2 as id_2,
        CASE
            WHEN forward_paths > 0 OR reverse_paths > 0 THEN TRUE
            ELSE FALSE
        END as is_related,
        CASE
            WHEN forward_hops IS NULL AND reverse_hops IS NULL THEN NULL
            WHEN forward_hops IS NULL THEN reverse_hops
            WHEN reverse_hops IS NULL THEN forward_hops
            ELSE LEAST(forward_hops, reverse_hops)
        END as shortest_hops,
        COALESCE(forward_paths, 0) as forward_paths,
        COALESCE(reverse_paths, 0) as reverse_paths,
        (COALESCE(forward_paths, 0) + COALESCE(reverse_paths, 0)) as total_paths,
        CASE
            WHEN forward_hops IS NULL AND reverse_hops IS NULL THEN NULL
            WHEN forward_hops IS NULL THEN reverse_path
            WHEN reverse_hops IS NULL THEN forward_path
            WHEN forward_hops <= reverse_hops THEN forward_path
            ELSE reverse_path
        END as shortest_path,
        CASE
            WHEN forward_paths = 0 AND reverse_paths = 0 THEN 0.0000
            ELSE (
                COALESCE(forward_paths / (forward_hops * forward_hops), 0) +
                COALESCE(reverse_paths / (reverse_hops * reverse_hops), 0)
            )
        END as relationship_strength,
        CASE
            WHEN forward_paths > 0 AND reverse_paths > 0 THEN 'bidirectional'
            WHEN forward_paths > 0 THEN 'forward_only'
            WHEN reverse_paths > 0 THEN 'reverse_only'
            ELSE 'none'
        END as relationship_type
    FROM (
        SELECT
            (SELECT MIN(hop_count) FROM v_id_paths WHERE start_v_id = id1 AND end_v_id = id2) as forward_hops,
            (SELECT COUNT(*) FROM v_id_paths WHERE start_v_id = id1 AND end_v_id = id2) as forward_paths,
            (SELECT MIN(path) FROM v_id_paths WHERE start_v_id = id1 AND end_v_id = id2 ORDER BY hop_count LIMIT 1) as forward_path,
            (SELECT MIN(hop_count) FROM v_id_paths WHERE start_v_id = id2 AND end_v_id = id1) as reverse_hops,
            (SELECT COUNT(*) FROM v_id_paths WHERE start_v_id = id2 AND end_v_id = id1) as reverse_paths,
            (SELECT MIN(path) FROM v_id_paths WHERE start_v_id = id2 AND end_v_id = id1 ORDER BY hop_count LIMIT 1) as reverse_path
    ) as relationship_data;
END$$

DELIMITER ;

DELIMITER $$

DROP PROCEDURE IF EXISTS GetTopXRelationships$$

CREATE PROCEDURE GetTopXRelationships(
    IN input_v_id VARCHAR(255),
    IN x_limit INT
)
BEGIN
    SELECT
        input_v_id as source_v_id,
        other_v_id as related_v_id,
        CASE
            WHEN forward_paths > 0 OR reverse_paths > 0 THEN TRUE
            ELSE FALSE
        END as is_related,
        CASE
            WHEN forward_hops IS NULL AND reverse_hops IS NULL THEN NULL
            WHEN forward_hops IS NULL THEN reverse_hops
            WHEN reverse_hops IS NULL THEN forward_hops
            ELSE LEAST(forward_hops, reverse_hops)
        END as shortest_hops,
        COALESCE(forward_paths, 0) as forward_paths,
        COALESCE(reverse_paths, 0) as reverse_paths,
        (COALESCE(forward_paths, 0) + COALESCE(reverse_paths, 0)) as total_paths,
        CASE
            WHEN forward_hops IS NULL AND reverse_hops IS NULL THEN NULL
            WHEN forward_hops IS NULL THEN reverse_path
            WHEN reverse_hops IS NULL THEN forward_path
            WHEN forward_hops <= reverse_hops THEN forward_path
            ELSE reverse_path
        END as shortest_path,
        CASE
            WHEN forward_paths = 0 AND reverse_paths = 0 THEN 0.0000
            ELSE (
                COALESCE(forward_paths / POWER(forward_hops, 2), 0) +
                COALESCE(reverse_paths / POWER(reverse_hops, 2), 0)
            )
        END as relationship_strength,
        CASE
            WHEN forward_paths > 0 AND reverse_paths > 0 THEN 'bidirectional'
            WHEN forward_paths > 0 THEN 'forward_only'
            WHEN reverse_paths > 0 THEN 'reverse_only'
            ELSE 'none'
        END as relationship_type
    FROM (
        SELECT
            other_ids.v_id as other_v_id,
            (SELECT MIN(hop_count) FROM v_id_paths
             WHERE BINARY start_v_id = BINARY input_v_id
             AND BINARY end_v_id = BINARY other_ids.v_id) as forward_hops,
            (SELECT COUNT(*) FROM v_id_paths
             WHERE BINARY start_v_id = BINARY input_v_id
             AND BINARY end_v_id = BINARY other_ids.v_id) as forward_paths,
            (SELECT MIN(path) FROM v_id_paths
             WHERE BINARY start_v_id = BINARY input_v_id
             AND BINARY end_v_id = BINARY other_ids.v_id
             ORDER BY hop_count LIMIT 1) as forward_path,
            (SELECT MIN(hop_count) FROM v_id_paths
             WHERE BINARY start_v_id = BINARY other_ids.v_id
             AND BINARY end_v_id = BINARY input_v_id) as reverse_hops,
            (SELECT COUNT(*) FROM v_id_paths
             WHERE BINARY start_v_id = BINARY other_ids.v_id
             AND BINARY end_v_id = BINARY input_v_id) as reverse_paths,
            (SELECT MIN(path) FROM v_id_paths
             WHERE BINARY start_v_id = BINARY other_ids.v_id
             AND BINARY end_v_id = BINARY input_v_id
             ORDER BY hop_count LIMIT 1) as reverse_path
        FROM (
            SELECT DISTINCT start_v_id as v_id FROM v_id_paths
            UNION
            SELECT DISTINCT end_v_id as v_id FROM v_id_paths
        ) as other_ids
        WHERE BINARY other_ids.v_id != BINARY input_v_id
    ) as relationship_data_all
    HAVING relationship_strength >= 0.01
    ORDER BY relationship_strength DESC
    LIMIT x_limit;

END$$

DELIMITER ;