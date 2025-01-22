package org.edu_sharing.service.oai.formats;

import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import org.alfresco.service.cmr.repository.NodeRef;
import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.util.Map;

/**
 * The {@code OaiMetadataFormatWriter} interface defines the contract for classes
 * that implement the writing of metadata in specific formats for the Open Archives
 * Initiative Protocol for Metadata Harvesting (OAI-PMH).
 *
 * <p>Implementations of this interface are responsible for specifying the metadata
 * format and writing metadata corresponding to a given node in a repository,
 * along with its associated properties, to an output stream.</p>
 *
 * <h2>Key Responsibilities</h2>
 * <ul>
 *   <li>Provide details of the metadata format using {@link MetadataFormat}.</li>
 *   <li>Write metadata to an output stream based on the repository node and its properties.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>Classes implementing this interface should be integrated into systems that
 * require OAI-PMH metadata harvesting, ensuring metadata is properly formatted
 * according to the specified standard.</p>
 */
public interface OaiMetadataFormatWriter {

    /**
     * Retrieves the metadata format supported by the implementation.
     *
     * @return a {@link MetadataFormat} object representing the metadata format,
     *         including namespace, schema location, and transformation logic.
     */
    @NotNull MetadataFormat getFormat();


    /**
     * Writes the metadata for a given repository node to the specified output stream.
     *
     * @param outputStream the {@link OutputStream} to which the metadata is written.
     * @param nodeId the {@link NodeRef} of the repository node whose metadata is being written.
     * @param properties a {@link Map} containing additional properties associated with the node.
     * @throws Exception if an error occurs during metadata generation or writing.
     */
    void write(@NotNull OutputStream outputStream, @NotNull NodeRef nodeId, @NotNull Map<String, Object> properties) throws Exception;
}
