package com.ecommerce.productcatalog.application;

import com.ecommerce.core.application.ICommand;
import java.util.List;
import java.util.UUID;

public record GetProductSnapshotsCommand(List<UUID> productIds) implements ICommand<Void> {
}
