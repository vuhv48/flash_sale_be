package io.github.vuhv48.flashsale.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "document_types")
public class DocumentTypeEntity extends BaseAuditableEntity {

	@Id
	private UUID id;

	@Column(nullable = false, length = 100)
	private String code;

	@Column(nullable = false, length = 255)
	private String name;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "max_size_mb")
	private Integer maxSizeMb;

	@Column(name = "allowed_mime_pattern", length = 255)
	private String allowedMimePattern;

	@Column(name = "max_files_per_ref")
	private Integer maxFilesPerRef;

	@Column(name = "default_visibility", nullable = false, length = 10)
	private String defaultVisibility;
}
