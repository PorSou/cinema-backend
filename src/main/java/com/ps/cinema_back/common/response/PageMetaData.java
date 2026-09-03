package com.ps.cinema_back.common.response;
import lombok.*;
import org.springframework.data.domain.Page;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageMetaData {

    private int totalPage;
    private int page;
    private long totalCount;
    private int pageSize;

    public static PageMetaData from(Page<?> page) {
        return PageMetaData.builder()
                .totalPage(page.getTotalPages())
                .page(page.getNumber())
                .totalCount(page.getTotalElements())
                .pageSize(page.getSize())
                .build();
    }
}
