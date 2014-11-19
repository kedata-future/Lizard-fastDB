package com.lizard.fastdb;

@SuppressWarnings("serial")
public class LizardException extends RuntimeException
{
	public LizardException() {
	}

	public LizardException( String msg ) {
		super( msg );
	}

	public LizardException( String msg, Throwable cause ) {
		super( msg, cause );
	}
	
	public LizardException( Throwable cause ) {
		super( cause );
	}
}
