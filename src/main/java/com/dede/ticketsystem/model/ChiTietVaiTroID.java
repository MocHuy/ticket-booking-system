package com.dede.ticketsystem.model;

import lombok.*;
import java.io.Serializable;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class ChiTietVaiTroID implements Serializable {
    private String maND;
    private String maVaiTro;
}