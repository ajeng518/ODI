package com.homegravity.Odi.domain.place.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "장소 검색 DTO")
@Getter
@Setter
@NoArgsConstructor
public class PlaceSearchDto {

    @Schema(description = "검색 내용")
    private String query;

    @Schema(description = "현재 위치 위도")
    private Double latitude;

    @Schema(description = "현재 위치 경도")
    private Double longitude;
}
