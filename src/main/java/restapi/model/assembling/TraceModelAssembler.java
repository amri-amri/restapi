package restapi.model.assembling;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;
import restapi.control.logic.DatabaseController;
import restapi.model.Trace;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Assembler to help produce a better representation of traces.
 */
@Component
public
class TraceModelAssembler implements RepresentationModelAssembler<Trace, EntityModel<Trace>> {

    @NotNull
    @Override
    public EntityModel<Trace> toModel(@Nullable Trace t) {

        if (t == null) {
            return EntityModel.of(
                    new Trace("null","null")
            );
        }

        return EntityModel.of(
                t,
                linkTo(methodOn(DatabaseController.class).one(t.id())).withSelfRel(),
                linkTo(methodOn(DatabaseController.class).all()).withRel("traces")
        );
    }
}