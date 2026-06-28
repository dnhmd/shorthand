package com.shorthand.consumer.infrastructure.adapter.outbound.database;

import com.shorthand.consumer.domain.model.ClickMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClickAnalyticsJpaRepository extends JpaRepository<ClickEventEntity, Long> {

    long countByLinkCode(String code);

    @Query("SELECT new com.shorthand.consumer.domain.model.ClickMetric(" +
            "CAST(FUNCTION('DATE_TRUNC', 'day', c.clickedAt) AS string), COUNT(c))" +
            "FROM ClickEventEntity c WHERE c.linkCode = :code " +
            "GROUP BY FUNCTION('DATE_TRUNC', 'day', c.clickedAt) " +
            "ORDER BY FUNCTION('DATE_TRUNC', 'day', c.clickedAt) ASC")
    List<ClickMetric> countByDate(@Param("code") String code);

    @Query("SELECT new com.shorthand.consumer.domain.model.ClickMetric(c.country, COUNT(c)) " +
            "FROM ClickEventEntity c WHERE c.linkCode = :code GROUP BY c.country")
    List<ClickMetric> countByCountry(@Param("code") String code);

    @Query("SELECT new com.shorthand.consumer.domain.model.ClickMetric(c.browser, COUNT(c)) " +
            "FROM ClickEventEntity c WHERE c.linkCode = :code GROUP BY c.browser")
    List<ClickMetric> countByBrowser(@Param("code") String code);

    @Query("SELECT new com.shorthand.consumer.domain.model.ClickMetric(c.os, COUNT(c)) " +
            "FROM ClickEventEntity c WHERE c.linkCode = :code GROUP BY c.os")
    List<ClickMetric> countByOs(@Param("code") String code);

    @Query("SELECT new com.shorthand.consumer.domain.model.ClickMetric(c.device, COUNT(c)) " +
            "FROM ClickEventEntity c WHERE c.linkCode = :code GROUP BY c.device")
    List<ClickMetric> countByDevice(@Param("code") String code);
}
