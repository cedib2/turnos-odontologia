package com.turnosodontologia.publicbooking;

import java.util.List;

public record DisponibilidadResponse(Long profesionalId, Long sillonId, String fecha,
                                     String codigoTratamiento, List<HuecoResponse> huecos) {
}
