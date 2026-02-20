package com.nba.nbanonbettingapp.dto;

import java.util.List;

public record BdlResponseDTO<T>(
        List<T> data,
        BdlMetaDTO meta
) {}