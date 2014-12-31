package com.appspot.c_three_games.domain.war;

import java.util.ArrayList;
import java.util.List;

import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.ForbiddenException;
import com.googlecode.objectify.NotFoundException;

public class TxResult<ResultType> {
  private ResultType result;

  private List<Message> msgs;

  private Throwable exception;

  public TxResult(ResultType result) {
    this.result = result;
    this.msgs = new ArrayList<Message>();
  }

  public TxResult(ResultType result, List<Message> msgs) {
    this.result = result;
    this.msgs = msgs;
  }

  public TxResult(Throwable exception) {
    if (exception instanceof NotFoundException || exception instanceof ForbiddenException
      || exception instanceof ConflictException || exception instanceof BadRequestException) {
      this.exception = exception;
    } else {
      throw new IllegalArgumentException("Exception not supported.");
    }
  }

  public ResultType getResult() throws NotFoundException, ForbiddenException, ConflictException, BadRequestException {
    if (exception instanceof NotFoundException) {
      throw (NotFoundException) exception;
    }
    if (exception instanceof ForbiddenException) {
      throw (ForbiddenException) exception;
    }
    if (exception instanceof ConflictException) {
      throw (ConflictException) exception;
    }
    if (exception instanceof BadRequestException) {
      throw (BadRequestException) exception;
    }
    for (Message msg : msgs) {
      msg.send();
    }
    return result;
  }
}
